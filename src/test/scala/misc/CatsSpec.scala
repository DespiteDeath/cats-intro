package misc

import cats._
import cats.effect._
import cats.effect.std._
import cats.effect.testkit.TestControl
import cats.implicits._
import org.scalacheck.Gen
import weaver._
import weaver.scalacheck._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.control.NoStackTrace

object CatsSpec extends SimpleIOSuite with Checkers {

  test("print current thread") {
    def printCurrentThread(): Unit     = println(Thread.currentThread().getName)
    def printCurrentThreadIO: IO[Unit] = IO(printCurrentThread())

    def e1 = ExecutionContext.fromExecutor(runnable => new Thread(runnable, "th1").start())
    def e2 = ExecutionContext.fromExecutor(runnable => new Thread(runnable, "th2").start())
    def e3 = ExecutionContext.fromExecutor(runnable => new Thread(runnable, "th3").start())
    def e4 = ExecutionContext.fromExecutor(runnable => new Thread(runnable, "th4").start())
    def e5 = ExecutionContext.fromExecutor(runnable => new Thread(runnable, "th5").start())
    def e6 = ExecutionContext.fromExecutor(runnable => new Thread(runnable, "th6").start())

    for {
      f1 <- (printCurrentThreadIO >>
        printCurrentThreadIO.evalOn(e1) >>
        printCurrentThreadIO.evalOn(e2).map(_ => printCurrentThread()).evalOn(e3).map(_ => printCurrentThread()))
        .startOn(e6)
      r1 <- f1.joinWithNever
    } yield assert(r1 == ())
  }

  test("Implement timeout in terms of IO.racePair") {
    case object Timeout extends RuntimeException with NoStackTrace
    def timeout[A](io: IO[A], duration: FiniteDuration): IO[A] =
      IO.racePair(IO.sleep(duration), io).flatMap {
        case Left((_, fiberB)) =>
          fiberB.cancel >> IO.raiseError[A](Timeout)
        case Right((fiberA, Outcome.Succeeded(b))) =>
          fiberA.cancel >> b
      }
    TestControl.execute(timeout[Int](IO.sleep(11 seconds) >> IO.pure(10), 10 seconds)) flatMap {
      control =>
        for {
          _ <- control.tick
          _ <- control.advance(10 seconds)
          _ <- control.tick
          r <- control.results
        } yield assert(r.contains(Outcome.errored(Timeout)))
    }
  }

  test("Implement timeout in terms of IO.race") {
    case object Timeout extends RuntimeException with NoStackTrace
    def timeout[A](io: IO[A], duration: FiniteDuration): IO[A] =
      IO.race(IO.sleep(duration), io).flatMap {
        case Left(()) => IO.raiseError[A](Timeout)
        case Right(a) => IO.pure(a)
      }
    val p1: IO[Expectations] = TestControl
      .execute(timeout[Int](IO.sleep(11 seconds) >> IO.pure(10), 10 seconds))
      .flatMap { control =>
        for {
          _ <- control.tick
          _ <- control.advance(10 seconds)
          _ <- control.tick
          r <- control.results
        } yield assert(r.contains(Outcome.errored(Timeout)))
      }

    val p2: IO[Expectations] = TestControl
      .execute(timeout[Int](IO.sleep(9 seconds) >> IO.pure(9), 10 seconds))
      .flatMap { control =>
        for {
          _ <- control.tick
          _ <- control.advance(9 seconds)
          _ <- control.tick
          r <- control.results
        } yield assert(r.contains(Outcome.Succeeded(9)))
      }

    for {
      e1 <- p1
      e2 <- p2
    } yield Expectations(e1.run |+| e2.run)
  }

  loggedTest("ref") { log =>
    for {
      state  <- IO.ref(0)
      fibers <- state.update(_ + 1).start.replicateA(100)
      _      <- fibers.traverse(_.join).void
      value  <- state.get
      _      <- log.debug(s"The final value is: $value")
    } yield assert(value == 100)
  }

  test("run concurrently requires Concurrent") {
    def nice[F[_]: Concurrent]: F[Fiber[F, Throwable, Int]] =
      Concurrent[F].start(Applicative[F].pure(1))
    for {
      f <- nice[IO]
      r <- f.joinWithNever
    } yield assert(r == 1)
  }

  test("run concurrently requires Spawn") {
    def nice[F[_]: Spawn]: F[Fiber[F, Throwable, Int]] =
      Spawn[F].start(Applicative[F].pure(1))
    for {
      f <- nice[IO]
      r <- f.joinWithNever
    } yield assert(r == 1)
  }

  test("run concurrently requires Temporal") {
    def nice[F[_]: Temporal]: F[Fiber[F, Throwable, Int]] =
      Temporal[F].start(Applicative[F].pure(1))
    for {
      f <- nice[IO]
      r <- f.joinWithNever
    } yield assert(r == 1)
  }

  test("give lock") {
    def clock[F[_]: Clock]: F[FiniteDuration] = Clock[F].realTime

    TestControl.execute(clock[IO]) flatMap { control =>
      for {
        _ <- control.advance(11.hours)
        _ <- control.tick
        r <- control.results
      } yield assert(r.contains(Outcome.succeeded(11.hours)))
    }
  }

  test("gen integers") {
    forall(Gen.posNum[Int]) { a =>
      IO(expect(a > 0))
    }
  }

  loggedTest("run on CI") { log =>
    for {
      _ <- log.info(sys.env.toString)
      _ <- ignore("not on ci").unlessA(sys.env.contains("JENKINS"))
      x <- IO.delay(1)
      y <- IO.delay(2)
    } yield expect(x == y)
  }

  loggedTest("do the thing") { log =>
    TestControl.execute(IO.sleep(10.hours) >> IO.realTime) flatMap { control =>
      for {
        _ <- control.tick
        _ <- control.advance(10.hours)
        _ <- control.tick
        r <- control.results
      } yield assert(r.contains(Outcome.succeeded(10.hours)))
    }
  }

  def retry[A](ioa: IO[A], delay: FiniteDuration, max: Int, random: Random[IO]): IO[A] =
    if (max <= 1)
      ioa
    else
      ioa handleErrorWith { _ =>
        random.betweenLong(0L, delay.toNanos) flatMap { ns =>
          IO.println(delay) >> IO.sleep(ns.nanos) *> retry(ioa, delay * 2, max - 1, random)
        }
      }

  test("backoff appropriately between attempts") {
    case object TestException extends RuntimeException

    val action = IO.raiseError[Int](TestException)
    val program = Random.scalaUtilRandom[IO] flatMap { random =>
      retry(action, 1.minute, 5, random)
    }

    TestControl.execute(program).flatMap { control =>
      for {
        _ <- control.tick
        _ <- (1 to 4).toList.traverse { i =>
          for {
            interval <- control.nextInterval
            _        <- IO(assert(interval >= 0.nanos))
            _        <- IO(assert(interval < (1 << i).minute))
            _        <- control.advanceAndTick(interval)
          } yield ()
        }
        r <- control.results
      } yield assert(r.contains(Outcome.errored(TestException)))
    }
  }

  test("make and use resources") {
    def file(name: String): Resource[IO, String] =
      Resource.make(IO.pure(name))(str => IO.println(s"releasing $str"))

    def read(name: String) = IO.pure(name).flatTap(str => IO.println(s"reading $str"))
    def write(name: String, data: String) =
      IO.pure(name).flatTap(str => IO.println(s"writing $str, $data"))

    (
      for {
        in1 <- file("file1")
        in2 <- file("file2")
        out <- file("file3")
      } yield (in1, in2, out)
    ).use {
      case (file1, file2, file3) =>
        for {
          bytes1 <- read(file1)
          bytes2 <- read(file2)
          _      <- write(file3, bytes1 ++ bytes2)
        } yield assert(true)
    }
  }

  test("sort using pq") {
    val list = List(1, 4, 3, 7, 5, 2, 6, 9, 8)

    for {
      pq <- PQueue.bounded[IO, Int](10)
      _  <- list.traverse(pq.offer)
      l  <- List.fill(list.length)(()).traverse(_ => pq.take)
    } yield assert(List(1, 2, 3, 4, 5, 6, 7, 8, 9) == l)
  }

  test("covariant queue") {
    def covariant(list: List[Int]): IO[List[Long]] =
      for {
        q <- Queue.bounded[IO, Int](10)
        qOfLongs = Functor[QueueSource[IO, *]].map(q)(_.toLong)
        _ <- list.traverse(q.offer)
        l <- List.fill(list.length)(()).traverse(_ => qOfLongs.take)
      } yield l

    covariant(List(1, 4, 2, 3)).flatMap(IO.println(_)).map(_ => assert(true))
  }

  test("contravariant queue") {
    def contravariant(list: List[Boolean]): IO[List[Int]] =
      for {
        q <- Queue.bounded[IO, Int](10)
        qOfBools: QueueSink[IO, Boolean] =
          Contravariant[QueueSink[IO, *]].contramap(q)((b: Boolean) => if (b) 1 else 0)
        _ <- list.traverse(qOfBools.offer)
        l <- List.fill(list.length)(()).traverse(_ => q.take)
      } yield l

    contravariant(List(true, false)).flatMap(IO.println(_)).map(_ => assert(true))
  }

  test("contravariant box") {
    trait Box[-T] {
      def m(t: T): Unit
    }
    implicit object ContravariantForBox extends Contravariant[Box] {
      override def contramap[A, B](fa: Box[A])(f: B => A): Box[B] = (b: B) => fa.m(f(b))
    }
    object BoxInt extends Box[Int] {
      override def m(t: Int): Unit = println(t)
    }
    val boxBoolean: Box[Boolean] =
      Contravariant[Box].contramap(BoxInt)((b: Boolean) => if (b) 1 else 0)
    boxBoolean.m(false)
    IO(assert(true))
  }

  test("random1") {
    def dieRoll[F[_]: Functor: Random]: F[Int] =
      Random[F].betweenInt(0, 6).map(_ + 1) // `6` is excluded from the range

    for {
      r <- Random.scalaUtilRandom[IO]
      implicit0(k: Random[IO]) = r
      i <- dieRoll[IO]
    } yield assert(List(1, 2, 3, 4, 5, 6).contains(i))
  }

  test("random2") {
    def dieRoll[F[_]: Functor: Random]: F[Int] =
      Random[F].betweenInt(0, 6).map(_ + 1) // `6` is excluded from the range

    Random
      .scalaUtilRandom[IO]
      .flatMap { implicit random =>
        dieRoll[IO]
      }
      .map(i => assert(List(1, 2, 3, 4, 5, 6).contains(i)))
  }

  loggedTest("logging") { log =>
    val randomString: IO[String] = IO(scala.util.Random.alphanumeric.take(10).mkString)
    randomString
      .flatTap(str => log.debug(s"random string $str"))
      .map(str => assert(str == str.reverse.reverse))
  }
}

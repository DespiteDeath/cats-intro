package misc

import cats._
import cats.effect._
import cats.effect.std._
import cats.effect.testkit.TestControl
import cats.implicits._
import org.scalacheck.Gen
import weaver._
import weaver.scalacheck._

import java.util.concurrent.{ CompletableFuture, TimeUnit }
import scala.concurrent.duration._

/**
  * https://typelevel.org/blog/2020/10/30/concurrency-in-ce3.html
  * https://blog.rockthejvm.com/cats-effect-racing-fibers/
  */
object CatsSpec extends SimpleIOSuite with Checkers {

  test("conditionals") {
    val isWeekday = true
    for {
      _ <- IO.println("Working").whenA(isWeekday).replicateA(5)
      _ <- IO.println("Offwork").unlessA(isWeekday)
    } yield ()
  }

  test("right shark") {
    (
      (IO.sleep(100.millis) *> printCurrentThreadIO("1")) &>
      (IO.sleep(200.millis) *> printCurrentThreadIO("2")) &>
      (IO.sleep(300.millis) *> printCurrentThreadIO("3")) &>
      (IO.sleep(400.millis) *> printCurrentThreadIO("4")) &>
      (IO.sleep(500.millis) *> printCurrentThreadIO("5")) &>
      (IO.sleep(600.millis) *> printCurrentThreadIO("6"))
    ).timed >>= IO.println
  }

  test("rate limited") {
    def rateLimited[F[_]: MonadCancelThrow, A](semaphore: Semaphore[F], func: => F[A]): F[A] =
      semaphore.permit.use(_ => func)

    val r = Supervisor[IO].use { supervisor =>
      for {
        s <- Semaphore[IO](5)
        m <- supervisor.supervise(rateLimited(s, IO.sleep(2.second))).replicateA(6)
        _ <- m.traverse(_.join)
      } yield ()
    }
    r.timed >>= IO.println
  }

  test("defer") {
    for {
      start <- IO.deferred[Boolean]
      done  <- IO.deferred[Boolean]
      _ <- (IO.println("1.ready") *>
        start.get *>
        IO.println("2.doing it") *>
        IO.sleep(2.seconds) *>
        IO.println("3.done") *> done.complete(true)).background.use { outcome =>
        IO.sleep(2.seconds) *> IO.println("1.carry on ") *> start.complete(true) *> done.get *> IO
          .println("4.ok")
      }
    } yield ()
  }

  test("defer not completing") {
    for {
      gate <- IO.deferred[Boolean]
      r <-
        gate.get
          .timeout(1.second)
          .handleErrorWith(_ => IO.println("failed to get it") *> false.pure[IO])
    } yield assert(!r)
  }

  test("ref updates should be idempotent, i.e. not effectful") {
    for {
      ref <- Ref[IO].of(0)
      _ <- Supervisor[IO].use { supervisor =>
        supervisor.supervise(ref.update(_ + 1)).replicateA(100)
      }
      k <- ref.get
    } yield assert(k == 100)
  }

  test("parProduct") {
    IO.sleep(1.second).parProduct(IO.sleep(1.second)).void.map(_Passed)
  }

  test("parMapN") {
    def makeIO(r: Long): IO[Long] = IO.sleep(r.seconds) *> IO.pure(r)

    (makeIO(2), makeIO(4))
      .parMapN {
        case (m, n) => m + n
      }
      .timed
      .flatTap {
        case (t, _) => IO.println(t)
      }
      .map {
        case (_, t) => t
      }
      .map(t => assert(t == 6))
  }

  test("parMapN fail") {
    val dummyFailure = new Exception("dummy")
    val ioA          = IO.sleep(2.seconds) *> IO(println("Delayed!"))
    val ioB          = IO.raiseError[Unit](dummyFailure)

    (ioA, ioB)
      .parMapN((_, _) => ())
      .map(_ => assert(false))
      .handleError(err => assert(err eq dummyFailure))
  }

  test("1. LEAK: background") {
    val child = (IO.sleep(1.second) *> IO.println("done")).onCancel(IO.println("cancelled"))
    child.start.flatMap(_.join).timeout(100.millis).map(_Passed) //1 LEAK, see done, not cancelled
  }
  test("2. OK: background to avoid fiber leaks") {
    val child = (IO.sleep(1.second) *> IO.println("done")).onCancel(IO.println("cancelled"))
    child.background.use(_.map(_ => ())).timeout(100.millis).map(_Passed) //2 no done, see cancelled
  }
  test("3. OK: supervisor to avoid fiber leaks") {
    val child = (IO.sleep(1.second) *> IO.println("done")).onCancel(IO.println("cancelled"))
    Supervisor[IO]
      .use { supervisor =>
        supervisor.supervise(child).flatMap(_.join).timeout(100.millis)
      }
      .map(_Passed) //3 no done, see cancelled
  }

  test("background") {
    //The Fiber itself is a resource. If you do something like fa.start.void, then fa will just keep running forever and no one can stop it
    //So the runloop itself leaks
    val t1 = IO(1).start.void.start.flatMap(_.cancel) //leaks

    //basically, start exists as a primitive to support more complicated things
    val t2 = IO(1).background.use(_ => IO.unit).start.flatMap(_.cancel) //it's not a leak anymore

    (IO.println("hello") *> IO.sleep(1.second))
      .flatMap(_ => IO.println("flatMap"))
      .guarantee(IO.println("guarantee"))
      .onCancel(IO.println("onCancel"))
      .foreverM
      .background
      .surround {
        IO.sleep(5.seconds)
      }
  }

  test("ops >>(lazily evaluated) *>(strictly evaluated)") {
    val p1 = for {
      f <- (IO.sleep(3.seconds) >> IO.pure(1)).start
      _ <- f.cancel
      d <- f.join
    } yield assert(d == Outcome.canceled)

    val p2 = for {
      f <- IO.sleep(3.seconds).start
      _ <- f.cancel
      r <- f.joinWith(IO.println("cancelled"))
    } yield Passed

    val p3 = for {
      f <- IO.sleep(3.seconds).start
      _ <- f.cancel
      r <- f.join !> IO.pure(1)
    } yield assert(r == 1)

    p1 |+| p2 |+| p3
  }

  test("uncancelable and poll???") {
    //poll like a "cancelable" block
    val p1 = for {
      start <- IO.monotonic
      f <- IO.uncancelable { _ =>
        IO.sleep(3.seconds)
      }.start
      _   <- f.cancel
      end <- IO.monotonic
      duration: FiniteDuration = end - start
    } yield assert(duration.toNanos >= 3.seconds.toNanos)

    val p2 = for {
      start <- IO.monotonic
      f <- IO.uncancelable { poll =>
        poll(IO.sleep(100.seconds)).onCancel(IO.println("cancelled"))
      }.start
      _   <- f.cancel
      end <- IO.monotonic
    } yield assert(end - start <= 1.seconds)
    p1 |+| p2
  }

  test("async") {
    def myAsync[F[_]: Async]: F[Int] =
      Async[F].async { callback =>
        callback(1.asRight[Throwable])
        Applicative[F].pure(None) //cleanup function
      }

    myAsync[IO].map(i => assert(i == 1))
  }

  test("async cancelled") {
    def myAsync[F[_]: Async: Console]: F[Int] =
      Async[F].async { callback =>
        CompletableFuture.supplyAsync { () =>
          TimeUnit.SECONDS.sleep(10)
          callback(1.asRight[Throwable])
        }
        Console[F].println("cancelled").some.pure[F] //cleanup function
      }

    for {
      f <- myAsync[IO].start
      _ <- f.cancel
    } yield ()
  }

  test("print current thread") {
    for {
      f1 <- (printCurrentThreadIO() >>
        printCurrentThreadIO().evalOn(e1) >>
        printCurrentThreadIO()
          .evalOn(e2)
          .map(_ => unsafePrintCurrentThread())
          .evalOn(e3)
          .map(_ => unsafePrintCurrentThread()))
        .startOn(e6)
      r1 <- f1.joinWithNever
    } yield Passed
  }

  test("Implement parTraverse in terms of IO.both") {
    val m: IO[List[Int]] = List(1, 2, 3).traverse(IO.pure)
    val k: IO[List[Int]] = List(1, 2, 3).parTraverse(IO.pure)

    def myTraverse[A, B](as: List[A])(f: A => IO[B]): IO[List[B]] =
      as.foldLeft(IO.pure(List.empty[B])) {
        case (r, e) =>
          for {
            l <- r
            b <- f(e)
          } yield l :+ b
      }

    def parTraverse[A, B](as: List[A])(f: A => IO[B]): IO[List[B]] =
      as.grouped(2).foldLeft(IO.pure(List.empty[B])) {
        case (bl, List(a)) =>
          for {
            bl <- bl
            b  <- f(a)
          } yield bl :+ b
        case (bl, List(a1, a2)) =>
          for {
            bl <- bl
            (b1, b2) <-
              IO.both(f(a1).flatTap(_printCurrentThreadIO), f(a2).flatTap(_printCurrentThreadIO))
          } yield bl :+ b1 :+ b2
      }

    parTraverse(List(1, 2, 3))(IO.pure).map(list => assert(list == List(1, 2, 3)))
    parTraverse(List(1, 2, 3, 4, 5, 6, 7))(IO.pure).map(list =>
      assert(list == List(1, 2, 3, 4, 5, 6, 7))
    )
  }

  test("Implement timeout in terms of IO.racePair") {
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
          _ <- control.advanceAndTick(10 seconds)
          r <- control.results
        } yield assert(r.contains(Outcome.errored(Timeout)))
    }
  }

  test("Implement timeout in terms of IO.race") {
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
          _ <- control.advanceAndTick(10 seconds)
          r <- control.results
        } yield assert(r.contains(Outcome.errored(Timeout)))
      }

    val p2: IO[Expectations] = TestControl
      .execute(timeout[Int](IO.sleep(9 seconds) >> IO.pure(9), 10 seconds))
      .flatMap { control =>
        for {
          _ <- control.tick
          _ <- control.advanceAndTick(9 seconds)
          r <- control.results
        } yield assert(r.contains(Outcome.Succeeded(9)))
      }

    p1 |+| p2
  }

  loggedTest("ref") { log =>
    for {
      state  <- IO.ref(0)
      fibers <- (state.update(_ + 1) >> printCurrentThreadIO()).start.replicateA(100)
      _      <- fibers.traverse(_.join).void
      value  <- state.get
      _      <- log.debug(s"The final value is: $value")
    } yield assert(value == 100)
  }

  test("ref2") {
    Counter.make[IO].flatMap { c =>
      for {
        _ <- c.get.flatMap(IO.println)
        _ <- c.incr
        _ <- c.get.flatMap(IO.println)
        _ <- c.incr.replicateA(5).void
        _ <- c.get.flatMap(IO.println)
        k <- c.get
      } yield assert(k == 6)
    }
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

  test("do the thing") {
    TestControl.execute(IO.sleep(10.hours) >> IO.realTime) flatMap { control =>
      for {
        _ <- control.tick
        _ <- control.advanceAndTick(10.hours)
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
        } yield Passed
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

    covariant(List(1, 4, 2, 3)).flatMap(IO.println(_)).map(_Passed)
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

    contravariant(List(true, false)).flatMap(IO.println(_)).map(_Passed)
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
    IO(Passed)
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

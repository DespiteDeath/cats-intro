package misc

import cats._
import cats.effect._
import cats.effect.std._
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.testkit.TestControl
import cats.implicits._
import org.mockito.MockitoSugar
import org.scalacheck.Test.Passed
import org.scalacheck._
import org.scalacheck.effect.PropF
import org.scalacheck.effect.PropF.forAllF
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class CatsSpec extends AsyncFlatSpec with AsyncIOSpec with MockitoSugar with Matchers {

  behavior of "cats"

  it should "run concurrently requires Concurrent" in {
    def nice[F[_]: Concurrent]: F[Fiber[F, Throwable, Int]] =
      Concurrent[F].start(Applicative[F].pure(1))
    for {
      f <- nice[IO]
      _ <- f.join
    } yield assert(true)
  }

  it should "run concurrently requires Spawn" in {
    def nice[F[_]: Spawn]: F[Fiber[F, Throwable, Int]] =
      Spawn[F].start(Applicative[F].pure(1))
    for {
      f <- nice[IO]
      _ <- f.join
    } yield assert(true)
  }

  it should "run concurrently requires Temporal" in {
    def nice[F[_]: Temporal]: F[Fiber[F, Throwable, Int]] =
      Temporal[F].start(Applicative[F].pure(1))
    for {
      f <- nice[IO]
      _ <- f.join
    } yield assert(true)
  }

  it should "give lock" in {
    def clock[F[_]: Clock]: F[FiniteDuration] = Clock[F].realTime

    implicit object kk extends Clock[IO] {
      override def applicative: Applicative[IO] = Applicative[IO]

      override def monotonic: IO[FiniteDuration] = IO.pure(0 seconds)

      override def realTime: IO[FiniteDuration] = IO.pure(0 seconds)
    }

    clock[IO].map(d => assert(d == (0 seconds)))
  }

  it should "gen integers" in {
    val m: PropF[IO] = forAllF(Gen.alphaStr) { str =>
      IO.println(str)
    }
    m.check().map(it => assert(it.status == Passed))
  }

  it should "do the thing" in {
    def retry[A](ioa: IO[A], delay: FiniteDuration, max: Int, random: Random[IO]): IO[A] =
      if (max <= 1)
        ioa
      else
        ioa handleErrorWith { _ =>
          random.betweenLong(0L, delay.toNanos) flatMap { ns =>
            IO.sleep(ns.nanos) *> retry(ioa, delay * 2, max - 1, random)
          }
        }

    case object TestException extends RuntimeException

    var attempts = 0
    val action = IO {
      attempts += 1

      if (attempts != 3)
        throw TestException
      else
        "success!"
    }

    val program: IO[String] = Random.scalaUtilRandom[IO] flatMap { random =>
      retry(action, 1.minute, 5, random)
    }

    TestControl.executeEmbed(program).map(it => assert(it == "success!"))
  }

  it should "make and use resources" in {
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
        } yield ()
    }
  }

  it should "sort using pq" in {
    val list = List(1, 4, 3, 7, 5, 2, 6, 9, 8)

    for {
      pq <- PQueue.bounded[IO, Int](10)
      _  <- list.traverse(pq.offer)
      l  <- List.fill(list.length)(()).traverse(_ => pq.take)
    } yield assertResult(List(1, 2, 3, 4, 5, 6, 7, 8, 9))(l)
  }

  it should "covariant queue" in {
    def covariant(list: List[Int]): IO[List[Long]] =
      for {
        q <- Queue.bounded[IO, Int](10)
        qOfLongs = Functor[QueueSource[IO, *]].map(q)(_.toLong)
        _ <- list.traverse(q.offer)
        l <- List.fill(list.length)(()).traverse(_ => qOfLongs.take)
      } yield l

    covariant(List(1, 4, 2, 3)).flatMap(IO.println(_))
  }

  it should "contravariant queue" in {
    def contravariant(list: List[Boolean]): IO[List[Int]] =
      for {
        q <- Queue.bounded[IO, Int](10)
        qOfBools: QueueSink[IO, Boolean] =
          Contravariant[QueueSink[IO, *]].contramap(q)((b: Boolean) => if (b) 1 else 0)
        _ <- list.traverse(qOfBools.offer)
        l <- List.fill(list.length)(()).traverse(_ => q.take)
      } yield l

    contravariant(List(true, false)).flatMap(IO.println(_))
  }

  it should "contravariant box" in {
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

  it should "random1" in {
    def dieRoll[F[_]: Functor: Random]: F[Int] =
      Random[F].betweenInt(0, 6).map(_ + 1) // `6` is excluded from the range

    for {
      r <- Random.scalaUtilRandom[IO]
      implicit0(k: Random[IO]) = r
      i <- dieRoll[IO]
    } yield assert(List(1, 2, 3, 4, 5, 6).contains(i))
  }

  it should "random2" in {
    def dieRoll[F[_]: Functor: Random]: F[Int] =
      Random[F].betweenInt(0, 6).map(_ + 1) // `6` is excluded from the range

    Random
      .scalaUtilRandom[IO]
      .flatMap { implicit random =>
        dieRoll[IO]
      }
      .map(i => assert(List(1, 2, 3, 4, 5, 6).contains(i)))
  }
}

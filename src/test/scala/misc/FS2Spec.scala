package misc

import cats._
import cats.effect._
import cats.effect.kernel.MonadCancel
import cats.effect.std.{ Semaphore, Supervisor }
import cats.implicits._
import fs2._
import weaver._
import weaver.scalacheck._

import scala.concurrent.duration._

object FS2Spec extends SimpleIOSuite with Checkers {

  test("process in chunks") {
    def writeToSocket[F[_]: Async](chunk: Chunk[String]): F[Unit] =
      Async[F].async { callback =>
        println(s"[thread: ${Thread.currentThread().getName}] :: Writing $chunk to socket")
        callback(().asRight[Throwable])
        Applicative[F].pure(None) //cleanup
      }

    Stream((1 to 100).map(_.toString): _*)
      .chunkN(10)
      .covary[IO]
      .parEvalMapUnordered(10)(writeToSocket[IO])
      .compile
      .drain
  }

  test("parallel drain") {
    val s: Stream[Pure, Long] = Stream.range(1L, 5L)
    val compute: Stream[IO, Unit] =
      s.parEvalMap[IO, Unit](10)(i => IO.sleep(i.seconds) *> printCurrentThreadIO())

    compute.compile.drain.timed.flatMap(IO.println)
  }

  test("resource") {
    Stream
      .range(1, 10)
      .covary[IO]
      .compile
      .resource
      .toList
      .use(IO.println)
  }

  test("scan1") {
    val m = Stream.constant[IO, Int](1).scan1(_ + _)
    m.take(10).compile.toList >>= IO.println
  }

  test("concurrently???") {
    val data: Stream[IO, Int] = Stream.range(1, 10).covary[IO]
    Stream
      .eval(fs2.concurrent.SignallingRef[IO, Int](0))
      .flatMap(s => Stream(s).concurrently(data.evalMap(s.set)))
      .flatMap(_.discrete)
      .takeWhile(_ < 9, takeFailure = true)
      .compile
      .last
      .map(it => assert(it.contains(9)))
  }

  test("mics") {
    Stream.eval(IO.println("hello")).compile.drain.void
  }

}

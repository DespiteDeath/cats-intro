package misc

import cats._
import cats.effect._
import cats.effect.std._
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

    val m: IO[Unit] = Stream((1 to 100).map(_.toString): _*)
      .chunkN(10)
      .covary[IO]
      .parEvalMapUnordered(10)(writeToSocket[IO])
      .compile
      .drain

    m.map(_ => assert(true))
  }

  test("print start and end of a stream") {
    val now: IO[Long]         = IO[Long](System.currentTimeMillis())
    val m: Stream[Pure, Long] = Stream(1L, 2L, 3L, 4L, 5L)

    val compute: Stream[IO, Unit] = m.parEvalMap[IO, Unit](10)(i => IO.sleep(i seconds))

    now
      .bracket(_ => compute.compile.drain)(start => now.map(_ - start).map(println))
      .handleErrorWith(e => Console[IO].printStackTrace(e))
      .map(_ => assert(true))
  }
}

package example

import java.util.concurrent.TimeUnit

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import cats.effect._
import fs2._

import scala.concurrent.ExecutionContext

object Fs2Main extends IOApp {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  val m: Stream[Pure, Long] = Stream(1L, 2L, 3L, 4L, 5L)

  val compute: Stream[IO, Unit] = m.parEvalMap[IO, Unit](10)(i => IO(TimeUnit.SECONDS.sleep(i)))

  val now: IO[Long] = IO[Long](System.currentTimeMillis())

  override def run(args: List[String]): IO[ExitCode] =
    now.bracket(_ => compute.compile.drain)(start => now.map(_ - start).map(println))
      .handleErrorWith(e => IO(e.printStackTrace()))
      .as(ExitCode.Success)
}

package example

import scala.concurrent.duration._
import cats.implicits._
import cats.effect._
import fs2._

object Fs2Main extends IOApp {

  val m: Stream[Pure, Long] = Stream(1L, 2L, 3L, 4L, 5L)

  val compute: Stream[IO, Unit] = m.parEvalMap[IO, Unit](10)(i => IO.sleep(i seconds))

  override def run(args: List[String]): IO[ExitCode] =
    now
      .bracket(_ => compute.compile.drain)(start => now.map(_ - start).map(println))
      .handleErrorWith(e => IO(e.printStackTrace()))
      .as(ExitCode.Success)
}

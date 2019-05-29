package example

import java.util.concurrent.TimeUnit

import scala.concurrent._
import cats.implicits._
import cats.effect._

object EffectsMain extends IOApp {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  val compute: IO[Long] = {

    def makeIO(r: Long) = IO[Long] {
      TimeUnit.SECONDS.sleep(r)
      r
    }

    val two = makeIO(2)
    val three = makeIO(3)

    (two, three).parMapN {
      case (m, n) => m + n
    }

  }

  val now: IO[Long] = IO[Long](System.currentTimeMillis())

  override def run(args: List[String]): IO[ExitCode] =
    now.bracket(_ => compute)(start => now.map(_ - start).map(println))
      .handleErrorWith(e => IO(e.printStackTrace())).as(ExitCode.Success)
}

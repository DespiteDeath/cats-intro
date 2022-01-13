package misc.timed

import cats._
import cats.effect._
import cats.effect.implicits._
import cats.implicits._

import scala.concurrent.duration._
import scala.util.control.NoStackTrace

object main1 extends IOApp.Simple {

  def myTimed[A, F[_]: Temporal](k: F[A]) = Temporal[F].timeout(k.timed, 2 seconds)

  case class TimedError(duration: FiniteDuration, a: Throwable)
      extends Exception(duration.toString(), a)
      with NoStackTrace

  def myTimed1[A, F[_]](
      k: F[A]
  )(implicit me: MonadError[F, Throwable], temporal: Temporal[F]): F[(FiniteDuration, A)] =
    for {
      start <- temporal.monotonic
      a <- k.recoverWith { error =>
        temporal.monotonic >>= (finish => me.raiseError[A](TimedError(finish - start, error)))
      }
      finish <- temporal.monotonic
    } yield (finish - start, a)

  val val1                   = myTimed1(IO.sleep(1 second)) >>= IO.println
  val val2                   = myTimed1(IO.sleep(1 second) >> IO(throw new RuntimeException("failed"))) >>= IO.println
  val val3                   = myTimed1(IO.sleep(3 second)) >>= IO.println
  override def run: IO[Unit] = val2
}

package misc

import cats.effect._
import cats.{ Monad, MonadError }

object main41 extends IOApp.Simple {

  def s = {
    val t1 = MonadError[IO, Throwable]
    val t2 = Concurrent[IO]
    val t3 = Sync[IO]
    ()
  }

  def m[F[_]](implicit me: Async[F]) = {
    val kk = Monad[F]
    ???
  }

  val t = m[IO]

  override def run: IO[Unit] = IO.println("running")
}

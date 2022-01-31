package example

import cats.MonadError
import cats.data._
import cats.effect._
import cats.effect.implicits._
import cats.effect.std.{ Console, Random }
import cats.implicits._

import scala.concurrent.duration._

object FS2Main {
  import cats.effect.unsafe.implicits.global
  import fs2._
  import org.http4s._
  import org.http4s.blaze.client._
  import org.http4s.implicits._

  def main(args: Array[String]): Unit = {
    val simulation: IO[Unit] = BlazeClientBuilder[IO].resource.use { client =>
      val req = Request[IO](
        uri = uri"https://???/private/healthcheck"
      )
      Stream
        .awakeEvery[IO](1.second)
        .zipRight(Stream.repeatEval(client.status(req)))
        .evalMap(IO.println)
        .compile
        .drain
    }
    simulation.timeout(120.seconds).attempt.unsafeRunSync()
  }
}

object ParTraverse extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    NonEmptyList
      .of(1, 2, 3)
      .parTraverse(i => putStrLine[IO](i.toString))
      .as(ExitCode.Success)
}

object ParFailFast extends IOApp {

  def make[F[_]: Sync: Console: Random: LiftIO](s: String, d: FiniteDuration): F[String] = {
    val t = for {
      _    <- printCurrentThread[F](s"start io$s ${d.toSeconds}")
      bool <- Random[F].nextBoolean
      _    <- MonadError[F, Throwable].raiseWhen(bool)(new IllegalArgumentException(s"$s failed"))
      _    <- IO.sleep(d).to[F]
      _    <- Console[F].println(s"done io$s")
    } yield s

    t.onCancel(Console[F].println(s"$s cancelled"))
  }

  def run(args: List[String]): IO[ExitCode] =
    Random
      .scalaUtilRandom[IO]
      .flatMap { implicit random =>
        (make[IO]("a", 11 second), make[IO]("b", 5 second), make[IO]("c", 3 second))
          .parMapN {
            case (m, n, o) => s"$m - $n - $o"
          }
          .map(println)
      }
      .as(ExitCode.Success)

  def makeIO1(s: String, d: FiniteDuration): IO[String] =
    (printCurrentThread[IO](s"start io$s ${d.toSeconds}") *>
    IO(s) <* IO.sleep(d) <* putStrLine[IO](s"done io$s")).onCancel(IO.println(s"$s cancelled"))

  def run1(args: List[String]): IO[ExitCode] =
    (IO.race(makeIO1("a", 11 second), makeIO1("b", 5 second)) >>= IO.println).as(ExitCode.Success)

  //  val handler: PartialFunction[Throwable, IO[String]] = {
//    case e => putStrLine[IO](s"parFailFast-handler: $e") *> IO.raiseError(e)
//  }
//
//  val logic: IO[Either[Throwable, List[Either[Throwable, String]]]] = List(a, b, c).parTraverse {
//    fa =>
//      Deferred[IO, Either[Throwable, String]].flatMap { d =>
//        Concurrent[IO].start(fa.recoverWith(handler).attempt.flatMap(d.complete)) *> d.get
//      }
//  }.attempt
//
//  def run(args: List[String]): IO[ExitCode] =
//    now
//      .bracket(_ => logic)(start => now.map(_ - start).map(println))
//      .as(ExitCode.Success)

}

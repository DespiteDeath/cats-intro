package misc

import cats.effect.{ ExitCode, IO, IOApp }
import misc.FieldValidation.ValidatedLogin
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.circe._

import scala.concurrent.ExecutionContext.global

object LoginServer extends IOApp {

  import Login._
  implicit val t: EntityDecoder[IO, ValidatedLogin] = accumulatingJsonOf[IO, ValidatedLogin]
  implicit val k: EntityEncoder[IO, ValidatedLogin] = jsonEncoderOf[IO, ValidatedLogin]

  val helloWorldService = HttpRoutes
    .of[IO] {
      case request @ POST -> Root / "login" =>
        request
          .as[ValidatedLogin]
          .handleErrorWith(IO.raiseError)
          .flatMap(Ok(_))
    }
    .orNotFound

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .withExecutionContext(global)
      .bindHttp(8080, "localhost")
      .withHttpApp(helloWorldService)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}

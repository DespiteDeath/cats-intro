package misc

import cats._
import cats.effect._
import cats.syntax.all._
import com.datastax.oss.driver.api.core.CqlSession
import com.ringcentral.cassandra4io.CassandraSession
import com.ringcentral.cassandra4io.cql._
import io.circe.generic.semiauto._
import io.circe.{ Decoder, Encoder }
import org.http4s._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._

import java.net.InetSocketAddress
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._
import scala.jdk.DurationConverters._

final case class Model(id: Int, data: String)

object Model {
  implicit val modelDecoder: Decoder[Model] = deriveDecoder[Model]
  implicit val modelEncoder: Encoder[Model] = deriveEncoder[Model]
  implicit def entityDecoder[F[_]: Concurrent]: EntityDecoder[F, Model] =
    accumulatingJsonOf[F, Model]
  implicit def entityEncoder[F[_]]: EntityEncoder[F, Model] = jsonEncoderOf[F, Model]
}

trait Dao[F[_]] {
  def put(value: Model): F[Unit]
  def get(id: Int): F[Option[Model]]
}

object Dao {

  private val insertQuery =
    cqlt"insert into table (id, data) values (${Put[Int]}, ${Put[String]})"
      .config(_.setTimeout(1.second.toJava))
  private val selectQuery =
    cqlt"select id, data from table where id = ${Put[Int]}".as[Model]

  def apply[F[_]: Sync](session: CassandraSession[F]): F[Dao[F]] =
    for {
      insert <- insertQuery.prepare(session)
      select <- selectQuery.prepare(session)
    } yield new Dao[F] {
      override def put(value: Model): F[Unit] =
        insert(value.id, value.data).execute.void
      override def get(id: Int): F[Option[Model]] =
        select(id).config(_.setExecutionProfileName("default")).select.head.compile.last
    }
}

object AuditServer extends IOApp {

  val builder = CqlSession
    .builder()
    .addContactPoint(InetSocketAddress.createUnresolved("localhost", 9042))
    .withLocalDatacenter("datacenter1")
    .withKeyspace("awesome")

  def makeSession[F[_]: Async]: Resource[F, CassandraSession[F]] =
    CassandraSession.connect(builder)

  val daoIO: IO[Dao[IO]] = makeSession[IO].use(session => Dao[IO](session))

//  so, the general trick is: when you get that error, you need to pick the typeclasses which is further down the hierarchy that extends from the ones you need
//  so, you don't need both MonadError and Concurrent, just Concurrent
//    and you don't need Concurrent and Sync, you need Async
//  basically just F[_]: Async should do
//btw, if you need Clock and Concurrent, should you ask for Temporal?
  def helloWorldService[F[_]: Async](
      daoF: F[Dao[F]]
  ): HttpApp[F] =
    HttpRoutes
      .of[F] {
        case request @ POST -> Root / "login" =>
          implicit val m = Model.entityEncoder[F]
          val response: F[Response[F]] = for {
            dao      <- daoF
            model    <- request.as[Model]
            _        <- dao.put(model)
            response <- Applicative[F].pure(Response(status = Status.Created).withEntity(model))
          } yield response
          response.handleErrorWith(error =>
            Sync[F].blocking(error.printStackTrace()) >>
            MonadError[F, Throwable].raiseError[Response[F]](error)
          )
      }
      .orNotFound

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .withExecutionContext(global)
      .bindHttp(8080, "localhost")
      .withHttpApp(helloWorldService[IO](daoIO))
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}

package misc

import cats.effect._
import cats.effect.unsafe.implicits.global
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class AuditServerSpec
    extends AnyFlatSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with EitherValues {

  behavior of "Audit server"

  it should "serve" in {
    val model   = Model(1, "mert")
    val request = Request[IO](method = POST, uri = uri"/login").withEntity(model)
    val dao = new Dao[IO] {
      override def put(value: Model): IO[Unit] = IO.pure(model)

      override def get(id: Int): IO[Option[Model]] = ???
    }
    implicit val m             = Model.entityEncoder[IO]
    val app: HttpApp[IO]       = AuditServer.helloWorldService[IO](IO.pure(dao))
    val response: Response[IO] = app(request).unsafeRunSync()

    response.status shouldBe Status.Created
    response.as[Model].unsafeRunSync() shouldBe model

  }
}

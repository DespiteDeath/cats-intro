package misc

import cats.effect._
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
    import unsafe.implicits.global
    val model   = Model(1, "mert")
    val request = Request[IO](method = POST, uri = uri"/login").withEntity(model)
    AuditServer.helloWorldService(request).unsafeRunSync() shouldBe Created(model)
  }
}

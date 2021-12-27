package misc

import cats.data.Validated.Invalid
import cats.data._
import cats.implicits._
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import org.scalacheck.Gen
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.{ Clock, Instant, ZoneOffset, ZonedDateTime }
import scala.util.chaining._

class FieldValidationSpec
    extends AnyFlatSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with EitherValues {

  behavior of "Field validation"

  implicit override val generatorDrivenConfig = PropertyCheckConfiguration()

  val emailTooShort = for {
    numberOfElems <- Gen.choose(0, 9)
    chars         <- Gen.listOfN(numberOfElems, Gen.asciiPrintableChar)
  } yield chars.mkString

  val emailTooLong = for {
    numberOfElems <- Gen.choose(21, 100)
    chars         <- Gen.listOfN(numberOfElems, Gen.asciiPrintableChar)
  } yield chars.mkString

  it should "validate short email" in {
    import FieldValidation._

    forAll(emailTooShort) { email =>
      assertResult(Validated.invalidNel[FieldValidationError, String](EmailError.TooShort))(
        validateEmail(email)
      )
    }
  }

  it should "validate short email and password" in {
    import FieldValidation._

    val emailTooShortError =
      Validated.invalidNel[FieldValidationError, (String, String)](EmailError.TooShort)
    val passwordTooShortError =
      Validated.invalidNel[FieldValidationError, (String, String)](PasswordError.TooShort)
    val errors = emailTooShortError productL passwordTooShortError

    forAll(emailTooShort, emailTooShort) { (emailTooShort, passwordTooShort) =>
      assertResult(errors)(
        validateEmailPassword(emailTooShort, passwordTooShort)
      )
      validateEmailPassword(emailTooShort, passwordTooShort) match {
        case Invalid(errors) => println(errors.groupBy(_.field))
      }
    }
  }

  it should "validate long email" in {
    import FieldValidation._

    forAll(emailTooLong) { email =>
      assertResult(false)(validateEmail(email).isValid)
    }
  }

  it should "decode login" in {
    import io.circe.generic.auto._
    val nonEmptyString = Gen.alphaStr.suchThat(_.nonEmpty)

    forAll(nonEmptyString, nonEmptyString) { (email, password) =>
      val json = s"""{"email":"$email","password":"$password"}"""
      decode[Login](json).value shouldBe Login(email, password)
      json.pipe(parse).flatMap(Login.loginDecoder.decodeJson).value shouldBe Login(email, password)
      decode[Login](json).value.asJson.noSpaces shouldBe json
    }
  }

  case class MyDate(date: ZonedDateTime)
  object MyDate {
    implicit val encoder: Encoder[MyDate] = deriveEncoder
    implicit val decoder: Decoder[MyDate] = deriveDecoder
  }

  it should "decode date time" in {
    val fixedClock = Clock.fixed(Instant.now, ZoneOffset.UTC)
    decode[MyDate](
      s"""{"date":"${ZonedDateTime.now(fixedClock).toString}"}"""
    ).value shouldBe MyDate(
      ZonedDateTime.now(fixedClock)
    )
  }
}

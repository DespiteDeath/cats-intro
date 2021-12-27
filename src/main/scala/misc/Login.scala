package misc

import cats.data.Validated.{ Invalid, Valid }
import io.circe.generic.semiauto._
import io.circe.{ Decoder, Encoder, Json, JsonObject }
import misc.FieldValidation._

case class Login(email: String, password: String) {
  //println(productElementNames.zip(productIterator).toList)
}

object Login {
  implicit val encoderError: Encoder[FieldValidationError] = Encoder.instance { error =>
    Json.fromJsonObject(
      JsonObject(
        ("field", Json.fromString(error.field)),
        ("message", Json.fromString(error.message))
      )
    )
  }
  implicit val encoderLogin: Encoder[ValidatedLogin] = Encoder.instance {
    case Valid(a)   => deriveEncoder[Login].apply(a)
    case Invalid(e) => Encoder.encodeNonEmptyList[FieldValidationError].apply(e)
  }

  implicit val decoder: Decoder[ValidatedLogin] =
    Decoder.forProduct2("email", "password")(FieldValidation.validateEmailPassword)

  val loginDecoder: Decoder[Login] = deriveDecoder[Login]
  val loginEncoder: Encoder[Login] = deriveEncoder[Login]
}

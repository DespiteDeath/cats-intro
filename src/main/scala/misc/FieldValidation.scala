package misc

import cats.data._
import cats.implicits._

object FieldValidation {

  sealed trait FieldValidationError {
    def field: String
    def message: String
  }
  sealed trait EmailError extends FieldValidationError {
    final val field = "email"
  }
  sealed trait PasswordError extends FieldValidationError {
    final val field = "password"
  }

  object EmailError {
    case object TooShort extends EmailError {
      override def message: String = "too short"
    }

    case object TooLong extends EmailError {
      override def message: String = "too long"
    }
  }

  object PasswordError {
    case object TooShort extends PasswordError {
      override def message: String = "too short"
    }

    case object TooLong extends PasswordError {
      override def message: String = "too long"
    }
  }

  def validateEmail(email: String): ValidatedNel[FieldValidationError, String] = {
    val tooShort = Validated.condNel[FieldValidationError, String](
      email.length >= 10,
      email,
      EmailError.TooShort
    )
    val tooLong = Validated.condNel[FieldValidationError, String](
      email.length <= 20,
      email,
      EmailError.TooLong
    )
    List(tooLong, tooShort).reduce(_ <* _)
  }

  def validatePassword(password: String): ValidatedNel[FieldValidationError, String] = {
    val tooShort = Validated.condNel[FieldValidationError, String](
      password.length >= 10,
      password,
      PasswordError.TooShort
    )
    val tooLong = Validated.condNel[FieldValidationError, String](
      password.length <= 20,
      password,
      PasswordError.TooLong
    )
    tooShort *> tooLong
  }

  type ValidatedLogin = ValidatedNel[FieldValidationError, Login]

  def validateEmailPassword(
      email: String,
      password: String
  ): ValidatedLogin =
    (validateEmail(email), validatePassword(password)).mapN(Login.apply)

}

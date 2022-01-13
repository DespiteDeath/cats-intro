import cats.data.Validated.Invalid
import cats.data._
import cats.implicits._

import scala.concurrent._
import scala.util.control.NoStackTrace
implicit val cte = ExecutionContext.fromExecutor(_.run())

sealed trait DomainValidation {
  def errorMessage: String
}

case object UsernameHasSpecialCharacters extends DomainValidation {
  def errorMessage: String = "Username cannot contain special characters."
}

case object PasswordDoesNotMeetCriteria extends DomainValidation {
  def errorMessage: String =
    "Password must be at least 10 characters long, including an uppercase and a lowercase letter, one number and one special character."
}

case object FirstNameHasSpecialCharacters extends DomainValidation {
  def errorMessage: String = "First name cannot contain spaces, numbers or special characters."
}

case object LastNameHasSpecialCharacters extends DomainValidation {
  def errorMessage: String = "Last name cannot contain spaces, numbers or special characters."
}

case object AgeIsInvalid extends DomainValidation {
  def errorMessage: String = "You must be aged 18 and not older than 75 to use our services."
}

final case class RegistrationData(
    username: String,
    password: String,
    firstName: String,
    lastName: String,
    age: Int
)

sealed trait FormValidatorNec {

  type ValidationResult[A] = ValidatedNec[DomainValidation, A]

  private def validateUserName(userName: String): ValidationResult[String] =
    if (userName.matches("^[a-zA-Z0-9]+$")) userName.validNec
    else UsernameHasSpecialCharacters.invalidNec

  private def validatePassword(password: String): ValidationResult[String] =
    if (password.matches("(?=^.{10,}$)((?=.*\\d)|(?=.*\\W+))(?![.\\n])(?=.*[A-Z])(?=.*[a-z]).*$"))
      password.validNec
    else PasswordDoesNotMeetCriteria.invalidNec

  private def validateFirstName(firstName: String): ValidationResult[String] =
    if (firstName.matches("^[a-zA-Z]+$")) firstName.validNec
    else FirstNameHasSpecialCharacters.invalidNec

  private def validateLastName(lastName: String): ValidationResult[String] =
    if (lastName.matches("^[a-zA-Z]+$")) lastName.validNec
    else LastNameHasSpecialCharacters.invalidNec

  private def validateAge(age: Int): ValidationResult[Int] =
    if (age >= 18 && age <= 75) age.validNec else AgeIsInvalid.invalidNec

  def validateForm(
      username: String,
      password: String,
      firstName: String,
      lastName: String,
      age: Int
  ): ValidationResult[RegistrationData] =
    (
      validateUserName(username),
      validatePassword(password),
      validateFirstName(firstName),
      validateLastName(lastName),
      validateAge(age)
    ).mapN(RegistrationData)

}

object FormValidatorNec extends FormValidatorNec

val t = FormValidatorNec.validateForm("...", "...", "...", "...", 0)
t match {
  case Invalid(chain) =>
    println(chain)
    chain.map(println)
  case _ => println("ok")
}

object MyValidation {

  sealed trait Error
  case object Error1 extends RuntimeException with NoStackTrace with Error
  case object Error2 extends RuntimeException with NoStackTrace with Error
  case object Error3 extends RuntimeException with NoStackTrace with Error
  case object Error4 extends RuntimeException with NoStackTrace with Error
  case object Error5 extends RuntimeException with NoStackTrace with Error
  case object Error6 extends RuntimeException with NoStackTrace with Error
  case object Error7 extends RuntimeException with NoStackTrace with Error
  case object Error8 extends RuntimeException with NoStackTrace with Error
  case object Error9 extends RuntimeException with NoStackTrace with Error

  def v1(x: Int): ValidatedNel[Error, Int] = Validated.condNel(x <= 1, x, Error1)
  def v2(x: Int): ValidatedNel[Error, Int] = Validated.condNel(x <= 2, x, Error2)
  def v3(x: Int): ValidatedNel[Error, Int] = Validated.condNel(x <= 3, x, Error3)
  def v4(x: Int): ValidatedNel[Error, Int] = Validated.condNel(x <= 4, x, Error4)
  def v5(x: Int): ValidatedNel[Error, Int] = Validated.condNel(x <= 5, x, Error5)
  def v6(x: Int): ValidatedNel[Error, Int] = Validated.condNel(x <= 6, x, Error6)
  def v7(x: Int): ValidatedNel[Error, Int] = Validated.condNel(x <= 7, x, Error7)
  def v8(x: Int): ValidatedNel[Error, Int] = Validated.condNel(x <= 8, x, Error8)
  def v9(x: Int): ValidatedNel[Error, Int] = Validated.condNel(x <= 9, x, Error9)

  def v11(x: Int) =
    v1(x) andThen v2 andThen v3 andThen v4 andThen v5 andThen v6 andThen v7 andThen v8 andThen v9

  def v12(x: Int) =
    v1(x) <* v2(x) <* v3(x) <* v4(x) <* v5(x) <* v6(x) <* v7(x) <* v8(x) <* v9(x)
}

Range.inclusive(1, 10).toList.traverse(MyValidation.v12) match {
  case Invalid(e) => e.toList.tapEach(println)
}

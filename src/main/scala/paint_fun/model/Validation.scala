package paint_fun.model

import cats.data.ValidatedNel
import cats.implicits.catsSyntaxValidatedId
import paint_fun.model.ValidationErrors._

object Validation {
  def required(f: Field): String => AllErrorsOr[String] =
    s => if (s.isEmpty) Required(f).invalidNel else s.valid

  def maxLength(f: Field, max: Int): String => AllErrorsOr[String] =
    s => if (s.length > max) MaxLengthExceeded(f).invalidNel else s.valid

  def pattern(f: Field, p: String): String => AllErrorsOr[String] =
    s => if (!s.matches(p)) InvalidChars(f).invalidNel else s.valid
}

trait Field {
  def productPrefix: String
  def name: String = productPrefix.toLowerCase
}

trait ValidationError {
  val field: Field
  def productPrefix: String
  def name: String = productPrefix
}

final case class FieldValidation[T](field: Field, validate: T => AllErrorsOr[T])

object ValidationErrors {
  final case class Required(field: Field) extends ValidationError
  final case class MaxLengthExceeded(field: Field) extends ValidationError
  final case class InvalidChars(field: Field) extends ValidationError
  final case class AlreadyExists(field: Field) extends ValidationError

  type AllErrorsOr[T] = ValidatedNel[ValidationError, T]
}

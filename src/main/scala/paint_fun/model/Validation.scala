package paint_fun.model

import cats.data.ValidatedNel
import cats.implicits.catsSyntaxValidatedId
import io.circe.{Encoder, Json}
import paint_fun.model.ValidationError._

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

sealed trait ValidationError {
  def productPrefix: String
}

sealed trait FieldValidationError extends ValidationError {
  def field: Field
}

object ValidationError {
  final case class Required(field: Field) extends FieldValidationError
  final case class MaxLengthExceeded(field: Field) extends FieldValidationError
  final case class InvalidChars(field: Field) extends FieldValidationError
  final case class AlreadyExists(field: Field) extends FieldValidationError

  final case object InvalidCredentials extends ValidationError

  type AllErrorsOr[T] = ValidatedNel[ValidationError, T]

  implicit val jsonEncoder: Encoder[ValidationError] = {
    case e: FieldValidationError => Json.obj(e.field.name -> Json.fromString(e.productPrefix))
    case e => Json.fromString(e.productPrefix)
  }
}

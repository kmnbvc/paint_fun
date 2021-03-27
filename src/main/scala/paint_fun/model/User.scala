package paint_fun.model

import cats._
import cats.implicits._
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import paint_fun.model.Validation._
import paint_fun.model.ValidationErrors.AllErrorsOr

final case class User(login: String, name: String, password: String)

object User {
  implicit val jsonCodec: Codec[User] = deriveCodec

  implicit def entityEncoder[F[_] : Applicative]: EntityEncoder[F, User] = jsonEncoderOf
}

object UserValidation {
  case object Login extends Field
  case object Name extends Field
  case object Password extends Field

  val login = FieldValidation(Login, required(Login) |+| maxLength(Login, 36) |+| pattern(Login, "[a-zA-Z]+"))
  val name = FieldValidation(Name, required(Name) |+| maxLength(Name, 64))
  val password = FieldValidation(Password, required(Password))

  def validate(user: User): AllErrorsOr[User] = {
    (login.validate(user.login) *>
      name.validate(user.name) *>
      password.validate(user.password)).as(user)
  }
}

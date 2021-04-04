package paint_fun.model

import cats.implicits._
import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredCodec
import paint_fun.model.Validation._
import paint_fun.model.ValidationErrors.AllErrorsOr

final case class User(login: String, name: String = "", password: String)

object User {
  implicit val config: Configuration = Configuration.default.withDefaults

  implicit val jsonCodec: Codec[User] = deriveConfiguredCodec
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

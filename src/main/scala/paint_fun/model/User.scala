package paint_fun.model

import cats.implicits._
import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredCodec
import paint_fun.model.Validation._
import paint_fun.model.ValidationError._

final case class User(login: String, name: String = "", password: String)

object User {
  implicit val config: Configuration = Configuration.default.withDefaults

  implicit val jsonCodec: Codec[User] = deriveConfiguredCodec
}

object UserValidation {
  case object Login extends Field
  case object Name extends Field
  case object Password extends Field

  private val login = required(Login) |+| maxLength(Login, 36) |+| pattern(Login, "[a-zA-Z]+")
  private val name = required(Name) |+| maxLength(Name, 64)
  private val password = required(Password)
  private val credentials = (required(Login), required(Password))

  def validate(user: User): AllErrorsOr[User] = {
    (login(user.login) *>
      name(user.name) *>
      password(user.password)).as(user)
  }

  val loginAlreadyExists: AllErrorsOr[User] = AlreadyExists(Login).invalidNel
  val credentialsNotValid: AllErrorsOr[User] = InvalidCredentials.invalidNel

  def validateCredentials(user: User): AllErrorsOr[User] = {
    credentials.bifoldMap(_.apply(user.login), _.apply(user.password)).as(user)
  }
}

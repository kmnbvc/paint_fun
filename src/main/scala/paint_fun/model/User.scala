package paint_fun.model

import cats.Applicative
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

final case class User(login: String, name: String, password: String)

object User {
  implicit val jsonCodec: Codec[User] = deriveCodec

  implicit def entityEncoder[F[_] : Applicative]: EntityEncoder[F, User] = jsonEncoderOf
}

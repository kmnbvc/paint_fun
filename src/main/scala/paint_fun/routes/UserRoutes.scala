package paint_fun.routes

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import paint_fun.model.User
import paint_fun.persistence.UserStorage
import paint_fun.routes.Authenticator.AuthService
import tsec.authentication.{TSecAuthService, asAuthed}

object UserRoutes {

  def userRoutes[F[_] : Sync](repo: UserStorage[F], auth: Authenticator[F]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case req@POST -> Root / "user" / "create" => for {
        user <- req.as[User]
        result <- repo.save(user)
        resp <- result.fold(UnprocessableEntity(_), Ok(_))
      } yield resp

      case req@POST -> Root / "user" / "login" => for {
        user <- req.as[User]
        result <- repo.verifyCredentials(user)
        resp <- result.fold(Forbidden(_), user => auth.embed(Ok(), user))
      } yield resp
    }
  }

  def userAuthedRoutes[F[_] : Sync](): AuthService[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    TSecAuthService {
      case GET -> Root / "user" / "active" asAuthed user => Ok(user)
    }
  }
}

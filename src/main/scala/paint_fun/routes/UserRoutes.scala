package paint_fun.routes

import cats.Functor
import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import paint_fun.model.User
import paint_fun.persistence.UserStorage
import paint_fun.routes.Auth._
import tsec.authentication.{TSecAuthService, asAuthed}

object UserRoutes {

  def userRoutes[F[_] : Sync : Functor](repo: UserStorage[F]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "user" / login => OptionT(repo.find(login)).foldF(NotFound())(Ok(_))

      case req@POST -> Root / "user" / "create" => for {
        user <- req.as[User]
        result <- repo.save(user)
        resp <- result.fold(errs => UnprocessableEntity(errs.groupMap(_.field.name)(_.name)), Ok(_))
      } yield resp

      case req@POST -> Root / "user" / "login" => for {
        user <- req.as[User]
        valid <- repo.verify(user)
        resp <- if (valid) Ok(Auth.createToken(user).map(_.toEncodedString))
          else Forbidden("Invalid credentials")
      } yield resp
    }
  }

  def authedRoutes[F[_] : Sync](auth: Authenticator[F]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    val authedService: AuthService[F] = TSecAuthService {
      case GET -> Root / "api2" asAuthed user => Ok("api2 response")
    }

    auth.handler.liftService(authedService)
  }
}

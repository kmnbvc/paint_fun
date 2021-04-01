package paint_fun.route

import cats.Functor
import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import paint_fun.model.User
import paint_fun.persistence.{UserRepo}
import tsec.authentication.{TSecAuthService, asAuthed}

object UserRoutes {

  def userRoutes[F[_] : Sync : Functor](repo: UserRepo[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "user" / login => OptionT(repo.find(login)).foldF(NotFound())(Ok(_))

      case req@POST -> Root / "user" / "create" => for {
        user <- req.as[User]
        result <- repo.save(user)
        resp <- result.fold(errs => UnprocessableEntity(errs.groupMap(_.field.name)(_.name)), Ok(_))
      } yield resp

      case req@POST -> Root / "user" / "login" => {
        val user = req.as[User]
        // todo validate user credentials
        val token = Auth.createToken()
        Ok(token.pure[F])
      }
    }
  }

  def authedRoutes[F[_] : Sync](auth: MyAuthenticator[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    val authedService: auth.AuthService = TSecAuthService {
      case GET -> Root / "api2" asAuthed user => Ok("api2")
    }

    auth.routes(authedService)
  }
}
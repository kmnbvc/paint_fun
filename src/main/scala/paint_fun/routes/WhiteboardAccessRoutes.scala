package paint_fun.routes

import cats.effect.Sync
import cats.implicits._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers._
import org.http4s.implicits._
import paint_fun.model.AccessType
import paint_fun.persistence.WhiteboardAccessStorage
import paint_fun.routes.Authenticator.AuthService
import tsec.authentication.{TSecAuthService, asAuthed}

object WhiteboardAccessRoutes {
  def whiteboardAccessRoutes[F[_] : Sync](repo: WhiteboardAccessStorage[F]): AuthService[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    TSecAuthService {
      case GET -> Root / "access" / "init" / "owner-only" asAuthed user => for {
        uuid <- repo.create(user)
        resp <- SeeOther(`Location`(uri"/board/b" / uuid.toString))
      } yield resp

      case GET -> Root / "access" / "state" / UUIDVar(id) asAuthed _ => repo.state(id).flatMap(x => Ok(x.toList))

      case req@PUT -> Root / "access" / "set" / UUIDVar(id) asAuthed user => for {
        acessType <- req.request.as[AccessType]
        _ <- repo.set(id, acessType, user)
        resp <- Ok()
      } yield resp
    }
  }
}

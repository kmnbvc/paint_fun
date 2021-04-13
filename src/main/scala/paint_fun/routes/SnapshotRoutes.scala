package paint_fun.routes

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import paint_fun.model.Snapshot
import paint_fun.persistence.SnapshotStorage
import paint_fun.routes.Authenticator._
import tsec.authentication.{TSecAuthService, asAuthed}

object SnapshotRoutes {

  def snapshotRoutes[F[_] : Sync](repo: SnapshotStorage[F], auth: Authenticator[F]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    val authedService: AuthService[F] = TSecAuthService {
      case GET -> Root / "snapshots" / "list" asAuthed user => Ok(repo.find(user))

      case req@POST -> Root / "snapshots" / "save" asAuthed user => for {
        body <- req.request.as[Snapshot]
        snapshot = body.copy(user = user.login)
        result <- repo.save(snapshot)
        resp <- Ok(result)
      } yield resp

      case GET -> Root / "snapshots" / "restore" / name asAuthed _ => Ok(name)
    }

    auth.handler.liftService(authedService)
  }
}

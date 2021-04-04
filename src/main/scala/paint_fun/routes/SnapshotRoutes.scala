package paint_fun.routes

import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import paint_fun.persistence.SnapshotStorage
import paint_fun.routes.Auth._
import tsec.authentication.{TSecAuthService, asAuthed}

object SnapshotRoutes {

  def snapshotRoutes[F[_] : Sync](repo: SnapshotStorage[F], auth: Authenticator[F]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    val authedService: AuthService[F] = TSecAuthService {
      case GET -> Root / "snapshots" / user asAuthed _ => Ok(repo.snapshots(user))
      case req@PUT -> Root / "snapshots" / user / "save" asAuthed _ => Ok(repo.save(user, Nil))
    }

    auth.handler.liftService(authedService)
  }

}

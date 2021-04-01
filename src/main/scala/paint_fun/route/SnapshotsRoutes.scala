package paint_fun.route

import cats.effect.Sync
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRoutes, EntityEncoder}
import paint_fun.model.User
import paint_fun.persistence.SnapshotsRepo

object SnapshotsRoutes {

  implicit def stringListEntityEncoder[F[_]]: EntityEncoder[F, List[String]] = ???

  def routes[F[_] : Sync](repo: SnapshotsRepo[F]): AuthedRoutes[User, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    AuthedRoutes.of[User, F] {
      case GET -> Root / "snapshots" / user as _ => Ok(repo.snapshots(user))
      case req@PUT -> Root / "snapshots" / user / "save" as _ => Ok(repo.saveSnapshot(user, Nil))
    }
  }

}

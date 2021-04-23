package paint_fun.routes

import cats.effect.Sync
import cats.implicits._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.implicits.http4sLiteralsSyntax
import paint_fun.model.Snapshot
import paint_fun.persistence.SnapshotStorage
import paint_fun.routes.Authenticator._
import tsec.authentication.{TSecAuthService, asAuthed}

import java.util.UUID.randomUUID

object SnapshotRoutes {

  def snapshotRoutes[F[_] : Sync](repo: SnapshotStorage[F]): AuthService[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    TSecAuthService {
      case GET -> Root / "snapshots" / "list" / boardId asAuthed _ => Ok(repo.find(boardId))

      case req@POST -> Root / "snapshots" / "save" asAuthed _ => for {
        snapshot <- req.request.as[Snapshot]
        result <- repo.save(snapshot)
        resp <- result.fold(UnprocessableEntity(_), Ok(_))
      } yield resp

      case GET -> Root / "snapshots" / "restore" / boardId / name asAuthed _ => for {
        snapshot <- repo.get(boardId, name)
        newBoardId = randomUUID().toString
        _ <- repo.linkToRestoreFrom(newBoardId, snapshot)
        resp <- SeeOther(`Location`(uri"/b" / newBoardId))
      } yield resp
    }
  }
}

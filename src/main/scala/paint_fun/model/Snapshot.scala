package paint_fun.model

import cats.implicits._
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import paint_fun.model.Validation._
import paint_fun.model.ValidationError._

import java.util.UUID

final case class Snapshot(sourceBoardId: UUID, name: String, data: String)

object Snapshot {
  implicit val jsonCodec: Codec[Snapshot] = deriveCodec
}

object SnapshotValidation {
  case object SourceBoardId extends Field
  case object Name extends Field
  case object Data extends Field

  private val name = required(Name)
  private val data = required(Data)
  private val sourceBoardId = required(SourceBoardId)

  def validate(snapshot: Snapshot): AllErrorsOr[Snapshot] = {
    (name(snapshot.name) *>
      data(snapshot.data) *>
      sourceBoardId(snapshot.sourceBoardId.toString)).as(snapshot)
  }

  val nameAlreadyExists: AllErrorsOr[Snapshot] = AlreadyExists(Name).invalidNel
}

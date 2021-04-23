package paint_fun.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import paint_fun.model.ValidationError.AllErrorsOr

final case class Snapshot(whiteboardId: String, name: String, data: String)

object Snapshot {
  implicit val jsonCodec: Codec[Snapshot] = deriveCodec
}

object SnapshotValidation {
  case object WhiteboardId extends Field
  case object Name extends Field
  case object Data extends Field

  def validate(snapshot: Snapshot): AllErrorsOr[Snapshot] = ???

  val nameAlreadyExists: AllErrorsOr[Snapshot] = ???
}

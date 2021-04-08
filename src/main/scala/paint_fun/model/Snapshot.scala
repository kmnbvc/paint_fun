package paint_fun.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

final case class Snapshot(name: String, user: String, data: String)

object Snapshot {
  implicit val jsonCodec: Codec[Snapshot] = deriveCodec
}

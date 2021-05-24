package paint_fun.model

import io.circe._
import io.circe.generic.semiauto._
import io.circe.jawn.decode

import java.util.UUID

final case class BoardStroke(whiteboardId: UUID, data: BoardStrokeData) {
  def toJson: String = Encoder[BoardStroke].apply(this).noSpaces
}

final case class BoardStrokeData(x0: Double, y0: Double, x1: Double, y1: Double, color: String) {
  def toJson: String = Encoder[BoardStrokeData].apply(this).noSpaces
}

object BoardStroke {
  implicit val jsonCodec: Codec[BoardStroke] = deriveCodec

  def fromJson(text: String): Either[Exception, BoardStroke] = decode(text)
}

object BoardStrokeData {
  implicit val jsonCodec: Codec[BoardStrokeData] = deriveCodec

  def fromJson(text: String): Either[Exception, BoardStrokeData] = decode(text)
}

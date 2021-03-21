package paint_fun.model

import cats.Applicative
import io.circe.jawn.decode
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

final case class BoardStroke(whiteboardId: String, data: BoardStrokeData) {
  def toJson(stroke: BoardStroke): String = stroke.asJson.noSpaces
  override def toString: String = toJson(this)
}

final case class BoardStrokeData(x0: Double, y0: Double, x1: Double, y1: Double, color: String) {
  def toJson(data: BoardStrokeData): String = data.asJson.noSpaces
  override def toString: String = toJson(this)
}

object BoardStroke {
  implicit val jsonCodec: Codec[BoardStroke] = deriveCodec

  implicit def boardStrokeEntityEncoder[F[_] : Applicative]: EntityEncoder[F, BoardStroke] = jsonEncoderOf
  implicit def boardsListStrokeEntityEncoder[F[_] : Applicative]: EntityEncoder[F, List[BoardStroke]] = jsonEncoderOf

  def fromJson(text: String): Either[Error, BoardStroke] = decode(text)
}

object BoardStrokeData {
  implicit val jsonCodec: Codec[BoardStrokeData] = deriveCodec

  def fromJson(text: String): Either[Error, BoardStrokeData] = decode(text)
}

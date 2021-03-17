package paint_fun.model

import cats.Applicative
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

final case class BoardStroke(whiteboardId: String, data: BoardStrokeData) {
  override def toString: String = BoardStroke.toJson(this)
}

final case class BoardStrokeData(x0: Double, y0: Double, x1: Double, y1: Double, color: String) {
  override def toString: String = BoardStrokeData.toJson(this)
}

object BoardStroke {
  implicit val jsonCodec: Codec[BoardStroke] = deriveCodec

  implicit def boardStrokeEntityEncoder[F[_] : Applicative]: EntityEncoder[F, BoardStroke] = jsonEncoderOf
  implicit def boardsListStrokeEntityEncoder[F[_] : Applicative]: EntityEncoder[F, List[BoardStroke]] = jsonEncoderOf

  private def toJson(stroke: BoardStroke): String = stroke.asJson.noSpaces
}

object BoardStrokeData {
  implicit val jsonCodec: Codec[BoardStrokeData] = deriveCodec

  private def toJson(data: BoardStrokeData): String = data.asJson.noSpaces
}

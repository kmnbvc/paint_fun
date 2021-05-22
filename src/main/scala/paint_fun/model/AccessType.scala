package paint_fun.model

import doobie.Meta
import doobie.postgres.implicits.pgEnumStringOpt
import io.circe.Codec
import io.circe.generic.extras.semiauto.deriveEnumerationCodec

sealed trait AccessType

object AccessType {
  case object Anyone extends AccessType
  case object OwnerOnly extends AccessType

  def toEnum(e: AccessType): String = e match {
    case Anyone => "anyone"
    case OwnerOnly => "owner_only"
  }

  def fromEnum(s: String): Option[AccessType] = Option(s).collect {
    case "anyone" => Anyone
    case "owner_only" => OwnerOnly
  }

  implicit val jsonCodec: Codec[AccessType] = deriveEnumerationCodec
  implicit val dbMeta: Meta[AccessType] = pgEnumStringOpt("access_type_enum", AccessType.fromEnum, AccessType.toEnum)
}

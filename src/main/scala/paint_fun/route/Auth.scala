package paint_fun.route

import cats.data.{Kleisli, OptionT}
import cats.effect.{IO, Sync}
import cats.implicits._
import dev.profunktor.auth.JwtAuthMiddleware
import dev.profunktor.auth.jwt.{JwtAuth, JwtToken}
import io.circe.jawn.decode
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, HttpRoutes, Request, Response, Status}
import paint_fun.model.User
import paint_fun.persistence.UserRepo
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import tsec.authentication._
import tsec.common._

import java.time.Instant
import scala.collection.immutable.Map
import scala.concurrent.duration.DurationInt

object Auth {

  def middleware[F[_] : Sync]: AuthMiddleware[F, User] = JwtAuthMiddleware(JwtAuth.noValidation, authenticate)

  def permit[F[_] : Sync](roles: List[String], route: AuthedRoutes[User, F]): AuthedRoutes[User, F] = {
    permit(roles).flatMap(r => {
      if (r.status == Status.Ok) route
      else Kleisli.liftF(OptionT(Option(r).pure[F]))
    })
  }

  def permit[F[_] : Sync](roles: List[String]): AuthedRoutes[User, F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    AuthedRoutes.of {
      case _ as User(login, name, password) => Ok()
      case _ => Forbidden("Operation not permited")
    }
  }

  private def authenticate[F[_] : Sync]: JwtToken => JwtClaim => F[Option[User]] = token => claim => {
    val user = parseUser(claim.subject, claim.content)

    user.filter {
      case User(login, _, pwd) => true // todo check login/password
    }.pure[F]
  }

  private def parseUser(login: Option[String], json: String): Option[User] = {
    decodeJwtContent(json).flatMap { content =>
      for {
        loginValue <- login
        name <- content.get("name")
        pwd <- content.get("password")
      } yield User(loginValue, name, pwd)
    }
  }

  private def decodeJwtContent(json: String): Option[Map[String, String]] = {
    decode[Map[String, String]](json).toOption
  }

  def createToken(): String = {
    val claim = JwtClaim(
      expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond),
      issuedAt = Some(Instant.now.getEpochSecond)
    )
    val key = "secretKey"
    val algo = JwtAlgorithm.HS256

    val token = JwtCirce.encode(claim, key, algo)
    token
  }

  object manual_pbkdf2 {
    import javax.crypto._
    import javax.crypto.spec._
    import java.security._
    import java.util._

    val DefaultIterations = 10000
    val random = new SecureRandom()

    private def pbkdf2(password: String, salt: Array[Byte], iterations: Int): Array[Byte] = {
      val keySpec = new PBEKeySpec(password.toCharArray, salt, iterations, 256)
      val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
      keyFactory.generateSecret(keySpec).getEncoded
    }

    def hashPassword(password: String, salt: Array[Byte]): String = {
      val salt = new Array[Byte](16)
      random.nextBytes(salt)
      val hash = pbkdf2(password, salt, DefaultIterations)
      val salt64 = Base64.getEncoder.encodeToString(salt)
      val hash64 = Base64.getEncoder.encodeToString(hash)

      s"$DefaultIterations:$hash64:$salt64"
    }

    def checkPassword(password: String, passwordHash: String): Boolean = {
      passwordHash.split(":") match {
        case Array(it, hash64, salt64) if it.forall(_.isDigit) =>
          val hash = Base64.getDecoder.decode(hash64)
          val salt = Base64.getDecoder.decode(salt64)

          val calculatedHash = pbkdf2(password, salt, it.toInt)
          calculatedHash.sameElements(hash)

        case other => sys.error("Bad password hash")
      }
    }
  }
}

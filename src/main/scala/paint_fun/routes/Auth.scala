package paint_fun.routes

import cats.Id
import cats.effect.Sync
import paint_fun.model.User
import tsec.authentication.{AugmentedJWT, TSecAuthService}
import tsec.jws.mac.JWTMac
import tsec.jwt.JWTClaims
import tsec.mac.jca.{HMACSHA256, MacSigningKey}

import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Auth {

  type Algo = HMACSHA256
  type JWTAug = AugmentedJWT[Algo, User]
  type AuthService[F[_]] = TSecAuthService[User, JWTAug, F]

  private val defaultIterations = 10000
  private val random = new SecureRandom()

  private val keyLength = 256
  private val keyGen = HMACSHA256
  private val keyAlgoName = s"PBKDF2WithHmacSHA$keyLength"

  val signingKey: MacSigningKey[Algo] = keyGen.generateKey[Id]

  def createToken[F[_] : Sync](user: User): F[JWTMac[Algo]] = {
    val claims = JWTClaims(
      issuer = Some("paint-fun"),
      subject = Some(user.login),
      expiration = Some(Instant.MAX),
      issuedAt = Some(Instant.now()))
    JWTMac.build[F, Algo](claims, signingKey)
  }

  def hash(password: String): String = {
    val salt = random.generateSeed(16)
    val hash = pbkdf2(password, salt, defaultIterations)
    val (salt64, hash64) = (encode(salt), encode(hash))
    println(s"$defaultIterations:$hash64:$salt64")
    s"$defaultIterations:$hash64:$salt64"
  }

  def checkPassword(password: String, correctHash: String): Boolean = {
    def matchHashes(it: Int, password: String, hash64: String, salt64: String): Boolean =
      pbkdf2(password, decode(salt64), it).sameElements(decode(hash64))

    correctHash.split(":") match {
      case Array(it, hash64, salt64) if it.forall(_.isDigit) =>
        matchHashes(it.toInt, password, hash64, salt64)
      case _ => false
    }
  }

  private def pbkdf2(password: String, salt: Array[Byte], iterations: Int): Array[Byte] = {
    val keySpec = new PBEKeySpec(password.toCharArray, salt, iterations, keyLength)
    val keyFactory = SecretKeyFactory.getInstance(keyAlgoName)
    keyFactory.generateSecret(keySpec).getEncoded
  }

  private def encode(value: Array[Byte]): String = Base64.getEncoder.encodeToString(value)
  private def decode(value: String): Array[Byte] = Base64.getDecoder.decode(value)
}

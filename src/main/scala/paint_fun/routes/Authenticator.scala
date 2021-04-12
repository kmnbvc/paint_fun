package paint_fun.routes

import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import org.http4s.{Response, SameSite}
import paint_fun.model.User
import paint_fun.persistence.UserStorage
import tsec.authentication._
import tsec.cipher.symmetric.IvGen
import tsec.cipher.symmetric.jca._

import java.security.{NoSuchAlgorithmException, SecureRandom, Security}
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import scala.concurrent.duration.DurationInt

class Authenticator[F[_] : Sync](users: UserStorage[F]) {

  tsecWindowsFix()

  val settings: TSecCookieSettings = TSecCookieSettings(
    cookieName = "tsec-auth-cookie",
    secure = false,
    httpOnly = false,
    domain = Some("localhost"),
    path = Some("/"),
    sameSite = SameSite.Lax,
    extension = None,
    expiryDuration = 60.minutes,
    maxIdle = None
  )

  private val key: SecretKey[AES128GCM] = AES128GCM.unsafeGenerateKey
  implicit val ivGen: IvGen[F, AES128GCM] = AES128GCM.defaultIvStrategy

  val userStore: IdentityStore[F, String, User] = login => OptionT(users.find(login))
  val authenticator = EncryptedCookieAuthenticator.stateless(settings, userStore, key)
  val handler = SecuredRequestHandler(authenticator)

  def embed(resp: F[Response[F]], user: User): F[Response[F]] = {
    (resp, authenticator.create(user.login)).mapN(authenticator.embed)
  }

  //todo: remove
  private def tsecWindowsFix(): Unit = {
    try {
      SecureRandom.getInstance("NativePRNGNonBlocking")
      ()
    } catch {
      case _: NoSuchAlgorithmException =>
        val secureRandom = new SecureRandom()
        val defaultSecureRandomProvider = secureRandom.getProvider.get(s"SecureRandom.${secureRandom.getAlgorithm}")
        secureRandom.getProvider.put("SecureRandom.NativePRNGNonBlocking", defaultSecureRandomProvider)
        Security.addProvider(secureRandom.getProvider)
        ()
    }
  }
}

object Authenticator {
  type AuthService[F[_]] = TSecAuthService[User, AuthEncryptedCookie[AES128GCM, String], F]

  private val defaultIterations = 10000
  private val random = new SecureRandom()

  private val keyLength = 256
  private val keyAlgoName = s"PBKDF2WithHmacSHA$keyLength"

  def hash(password: String): String = {
    val salt = random.generateSeed(16)
    val hash = pbkdf2(password, salt, defaultIterations)
    val (salt64, hash64) = (encode(salt), encode(hash))
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

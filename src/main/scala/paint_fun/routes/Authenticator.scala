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

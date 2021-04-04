package paint_fun.routes

import cats.effect.Sync
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import paint_fun.model.User
import paint_fun.routes.Auth._
import tsec.authentication._

import java.security.{NoSuchAlgorithmException, SecureRandom, Security}
import scala.concurrent.duration.DurationInt

class Authenticator[F[_] : Sync] {

  val authenticator: JWTAuthenticator[F, User, User, Algo] =
    JWTAuthenticator.pstateless.inBearerToken[F, User, Algo](10.minutes, None, signingKey)

  val handler = SecuredRequestHandler(authenticator)

  private def tsecWindowsFix(): Unit =
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

  tsecWindowsFix()
}

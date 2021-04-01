package paint_fun.route

import cats.Id
import cats.effect.Sync
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import paint_fun.model.User
import tsec.authentication._
import tsec.mac.jca.{HMACSHA256, MacSigningKey}

import java.security.{NoSuchAlgorithmException, SecureRandom, Security}
import scala.concurrent.duration.DurationInt


class Authenticator[F[_] : Sync] {

  val signingKey: MacSigningKey[HMACSHA256] = HMACSHA256.generateKey[Id]

  val authenticator: JWTAuthenticator[F, User, User, HMACSHA256] =
    JWTAuthenticator.pstateless.inBearerToken[F, User, HMACSHA256](10.minutes, None, signingKey)

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

package paint_fun.route

import cats.data.OptionT
import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import paint_fun.model.User
import paint_fun.persistence.{AuthTokensRepo, UserRepo}
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import tsec.authentication._
import tsec.common.SecureRandomId

import java.time.Instant
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}


class MyAuthenticator[F[_]: Sync](
                              userRepo: UserRepo[F],
                              tokenRepo: AuthTokensRepo[F]) {

  type AuthService = TSecAuthService[User, TSecBearerToken[String], F]

  private val tokenStore = new DummyTokenStore

  private val userStore: IdentityStore[F, String, User] = login => {
    //OptionT(userRepo.find(login))
    OptionT(User(login, "stub user", "").some.pure[F])
  }

  val settings: TSecTokenSettings = TSecTokenSettings(expiryDuration = 10.minutes, maxIdle = None)
  val authenticator = BearerTokenAuthenticator(tokenStore, userStore, settings)
  val handler = SecuredRequestHandler(authenticator)

  def routes(svc: AuthService): HttpRoutes[F] = handler.liftService(svc)
}

class DummyTokenStore[F[_]](implicit S: Sync[F]) extends BackingStore[F, SecureRandomId, TSecBearerToken[String]] {

  private val storage = scala.collection.mutable.HashMap.empty[String, TSecBearerToken[String]]

  def put(token: TSecBearerToken[String]): F[TSecBearerToken[String]] = {
    println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!! put")
    val prev = storage.put(token.id, token)
    if (prev.isEmpty)
      token.pure[F]
    else
      S.raiseError(new IllegalArgumentException)
  }

  def update(token: TSecBearerToken[String]): F[TSecBearerToken[String]] = {
    println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!! update")
    storage.update(token.id, token)
    token.pure[F]
  }

  def delete(id: SecureRandomId): F[Unit] = {
    println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!! delete")
    S.delay(storage.remove(id).toRight(new IllegalArgumentException)).rethrow.void
  }

  def get(id: SecureRandomId): OptionT[F, TSecBearerToken[String]] = {
    println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!! get " + id)
    val claim = decodeJwt(id)

    val token = for {
      c <- claim
      jti <- c.jwtId
      sub <- c.subject
      token = TSecBearerToken(jti.asInstanceOf[SecureRandomId], sub, Instant.MAX, None)
      _ = storage.put(token.id, token)
    } yield token

    OptionT.fromOption(token.flatMap(t => storage.get(t.id)))
  }

  def decodeJwt(str: String): Option[JwtClaim] = {
    JwtCirce.decode(str, "your-256-bit-secret", Seq(JwtAlgorithm.HS256)) match {
      case Failure(e) => throw e
      case Success(claim) => Some(claim)
    }
  }
}

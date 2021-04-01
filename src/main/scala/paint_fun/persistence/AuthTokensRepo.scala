package paint_fun.persistence

import cats.data.OptionT
import cats.effect.{Async, ContextShift}
import org.slf4j.{Logger, LoggerFactory}
import tsec.authentication.{BackingStore, TSecBearerToken}
import tsec.common.SecureRandomId

trait AuthTokensRepo[F[_]] extends BackingStore[F, SecureRandomId, TSecBearerToken[String]] {

}

object AuthTokensRepo {
  def apply[F[_] : AuthTokensRepo]: AuthTokensRepo[F] = implicitly

  def instance[F[_] : Async : ContextShift]: AuthTokensRepo[F] = new AuthTokensRepoImpl[F]
}

class AuthTokensRepoImpl[F[_] : Async : ContextShift] extends DbConnection[F] with AuthTokensRepo[F] {

  override val logger: Logger = LoggerFactory.getLogger(getClass)

  override def put(elem: TSecBearerToken[String]): F[TSecBearerToken[String]] = ???

  override def update(v: TSecBearerToken[String]): F[TSecBearerToken[String]] = ???

  override def delete(id: SecureRandomId): F[Unit] = ???

  override def get(id: SecureRandomId): OptionT[F, TSecBearerToken[String]] = ???
}

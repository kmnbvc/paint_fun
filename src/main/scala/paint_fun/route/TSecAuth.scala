package paint_fun.route

import cats.data.OptionT
import cats.effect.{IO, Sync}
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import paint_fun.route.TSecAuth.ExampleAuthHelpers._
import tsec.authentication._
import tsec.common.SecureRandomId

import scala.collection.mutable
import scala.concurrent.duration._

object TSecAuth {

  object BearerTokenExample {
    type AuthService = TSecAuthService[User, TSecBearerToken[Int], IO]

    val bearerTokenStore: BackingStore[IO, SecureRandomId, TSecBearerToken[Int]] = dummyBackingStore(s => SecureRandomId.coerce(s.id))
    val userStore: BackingStore[IO, Int, User] = dummyBackingStore(_.id)
    val settings: TSecTokenSettings = TSecTokenSettings(expiryDuration = 10.minutes, maxIdle = None)
    val bearerTokenAuth = BearerTokenAuthenticator(bearerTokenStore, userStore, settings)
    val auth = SecuredRequestHandler(bearerTokenAuth)

    val authedService: AuthService = TSecAuthService {
      case GET -> Root / "api2" asAuthed user => Ok()
    }

    val liftedComposed: HttpRoutes[IO] = auth.liftService(authedService)
  }

  object ExampleAuthHelpers {
    def dummyBackingStore[F[_], I, V](getId: V => I)(implicit F: Sync[F]): BackingStore[F, I, V] = new BackingStore[F, I, V] {
      private val storageMap = mutable.HashMap.empty[I, V]

      def put(elem: V): F[V] = {
        val map = storageMap.put(getId(elem), elem)
        if (map.isEmpty)
          F.pure(elem)
        else
          F.raiseError(new IllegalArgumentException)
      }

      def get(id: I): OptionT[F, V] =
        OptionT.fromOption[F](storageMap.get(id))

      def update(v: V): F[V] = {
        storageMap.update(getId(v), v)
        F.pure(v)
      }

      def delete(id: I): F[Unit] =
        storageMap.remove(id) match {
          case Some(_) => F.unit
          case None => F.raiseError(new IllegalArgumentException)
        }
    }

    case class User(id: Int, age: Int, name: String)

  }
}

package paint_fun.persistence

import paint_fun.model._

trait UserRepo[F[_]] {
  def save(user: User): F[Either[Exception, User]]
  def find(id: String): F[Option[User]]
}

object UserRepo {
  def apply[F[_] : UserRepo]: UserRepo[F] = implicitly

  def instance[F[_]]: UserRepo[F] = new UserRepoImpl[F]
}

class UserRepoImpl[F[_]] extends UserRepo[F] {
  override def save(user: User): F[Either[Exception, User]] = ???

  override def find(id: String): F[Option[User]] = ???
}

package paint_fun.persistence

import doobie.implicits.toSqlInterpolator
import org.slf4j.{Logger, LoggerFactory}
import paint_fun.model._
import doobie.syntax._

trait UserRepo[F[_]] {
  def save(user: User): F[Either[Exception, User]]
  def find(id: Long): F[Option[User]]
}

object UserRepo {
  def apply[F[_] : UserRepo]: UserRepo[F] = implicitly

  def instance[F[_]]: UserRepo[F] = new UserRepoImpl[F]
}

class UserRepoImpl[F[_]] extends UserRepo[F] with DbConnection {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def save(user: User): F[Either[Exception, User]] = ???

  override def find(id: Long): F[Option[User]] = transact {
    sql"select * from paint_fun.users where id = $id".query[User].to[List]
  }.map(_.headOption)
}

package paint_fun.persistence

import cats.effect.{Async, ContextShift}
import cats.implicits._
import doobie.implicits.toSqlInterpolator
import org.slf4j.{Logger, LoggerFactory}
import paint_fun.model._

trait UserRepo[F[_]] {
  def save(user: User): F[User]
  def find(id: Long): F[Option[User]]
}

object UserRepo {
  def apply[F[_] : UserRepo]: UserRepo[F] = implicitly

  def instance[F[_] : Async : ContextShift]: UserRepo[F] = new UserRepoImpl[F]
}

class UserRepoImpl[F[_]](implicit
                         async: Async[F],
                         cs: ContextShift[F]
                        ) extends DbConnection[F] with UserRepo[F] {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def save(user: User): F[User] = transact {
    sql"insert into paint_fun.users (login, name) values(${user.login}, ${user.name})".update.run.map(_ => user)
  }

  override def find(id: Long): F[Option[User]] = transact {
    sql"select * from paint_fun.users where id = $id".query[User].to[List]
  }.map(_.headOption)
}

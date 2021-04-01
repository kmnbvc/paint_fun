package paint_fun.persistence

import cats.effect.{Async, ContextShift}
import cats.implicits._
import doobie.implicits._
import org.slf4j.{Logger, LoggerFactory}
import paint_fun.model.ValidationErrors.AllErrorsOr
import paint_fun.model._

trait UserRepo[F[_]] {
  def save(user: User): F[AllErrorsOr[User]]
  def find(login: String): F[Option[User]]
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

  def save(user: User): F[AllErrorsOr[User]] = {
    //todo check no such user
    UserValidation.validate(user).traverse(insert)
  }

  private def insert(user: User): F[User] = transact {
    sql"insert into paint_fun.users (login, name, password_hash) values(${user.login}, ${user.name}, ${"justpwd"})".update.run.as(user)
  }

  def find(login: String): F[Option[User]] = transact {
    sql"select * from paint_fun.users where login = $login".query[User].option
  }
}

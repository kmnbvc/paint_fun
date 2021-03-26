package paint_fun.persistence

import cats.data.Validated
import cats.effect.{Async, ContextShift}
import cats.implicits._
import doobie.implicits._
import org.slf4j.{Logger, LoggerFactory}
import paint_fun.model._

trait UserRepo[F[_]] {
  def save(user: User): F[Validated[String, User]]
  def find(login: String): F[Option[User]]
  def delete(login: String): F[Boolean]
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

  def save(user: User): F[Validated[String, User]] = {
    validate(user).traverse(insert)
  }

  private def insert(user: User): F[User] = transact {
    sql"insert into paint_fun.users (login, name) values(${user.login}, ${user.name})".update.run.as(user)
  }

  private def validate(user: User): Validated[String, User] = {
    def validateName: Validated[String, User] = {
      if (user.name.isEmpty) "name required".invalid
      else if (user.name.length > 64) "too long".invalid
      else user.valid
    }

    def validateLogin: Validated[String, User] = {
      if (user.login.isEmpty) "login required".invalid
      else if (user.login.length > 36) "too long".invalid
      else if (!user.login.matches("[a-zA-Z]+")) "invalid symbols".invalid
      else user.valid
    }

    def validatePassword: Validated[String, User] = {
      if (user.password.isEmpty) "password required".invalid
      else user.valid
    }

    validateName *> validateLogin *> validatePassword
  }

  def find(login: String): F[Option[User]] = transact {
    sql"select * from paint_fun.users where login = $login".query[User].option
  }

  def delete(login: String): F[Boolean] = transact {
    sql"delete from users where login = $login".update.run.map(_ > 0)
  }
}

package paint_fun.persistence

import cats.data.OptionT
import cats.data.Validated.{Invalid, Valid}
import cats.effect.{Async, ContextShift}
import cats.implicits._
import doobie.implicits._
import paint_fun.model.UserValidation.validate
import paint_fun.model.ValidationErrors._
import paint_fun.model._
import paint_fun.routes.Auth

trait UserStorage[F[_]] {
  def save(user: User): F[AllErrorsOr[User]]
  def find(login: String): F[Option[User]]
  def verify(user: User): F[Boolean]
}

object UserStorage {
  def apply[F[_] : UserStorage]: UserStorage[F] = implicitly

  def instance[F[_] : Async : ContextShift]: UserStorage[F] = new UserStorageImpl[F]
}

class UserStorageImpl[F[_]](implicit
                            async: Async[F],
                            cs: ContextShift[F]
                           ) extends DbConnection[F] with UserStorage[F] {

  def save(user: User): F[AllErrorsOr[User]] = validate(user) match {
    case Valid(_) => insert(user)
    case Invalid(e) => e.invalid[User].pure[F]
  }

  private def insert(user: User): F[AllErrorsOr[User]] = {
    val pwdHash = Auth.hash(user.password)
    val query = sql"insert into paint_fun.users (login, name, password_hash) values(${user.login}, ${user.name}, $pwdHash) on conflict do nothing"
    val res = query.update.run.map {
      case 0 => UserValidation.loginAlreadyExists
      case _ => user.validNel
    }
    transact(res)
  }

  def find(login: String): F[Option[User]] = transact {
    sql"select login, name from paint_fun.users where login = $login".query[(String, String)].map {
      case (login, name) => User(login, name, "")
    }.option
  }

  def verify(user: User): F[Boolean] = OptionT(transact {
    sql"select password_hash from paint_fun.users where login = ${user.login}".query[String].option
  }).map(Auth.checkPassword(user.password, _)).getOrElse(false)
}

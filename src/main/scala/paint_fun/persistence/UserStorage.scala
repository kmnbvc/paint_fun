package paint_fun.persistence

import cats.data.OptionT
import cats.data.Validated.{Invalid, Valid}
import cats.effect.{Async, ContextShift}
import cats.implicits._
import doobie.hikari.HikariTransactor
import doobie.implicits._
import paint_fun.model.UserValidation._
import paint_fun.model.ValidationError._
import paint_fun.model._
import paint_fun.routes.Authenticator._

trait UserStorage[F[_]] {
  def save(user: User): F[AllErrorsOr[User]]
  def find(login: String): F[Option[User]]
  def verifyCredentials(user: User): F[AllErrorsOr[User]]
}

object UserStorage {
  def apply[F[_] : UserStorage]: UserStorage[F] = implicitly

  def instance[F[_] : Async : ContextShift : HikariTransactor]: UserStorage[F] = new UserStorageImpl[F]
}

class UserStorageImpl[F[_]](implicit
                            async: Async[F],
                            cs: ContextShift[F],
                            xa: HikariTransactor[F]
                           ) extends UserStorage[F] {

  def save(user: User): F[AllErrorsOr[User]] = validate(user) match {
    case Valid(_) => insert(user)
    case Invalid(e) => e.invalid[User].pure[F]
  }

  private def insert(user: User): F[AllErrorsOr[User]] = {
    val pwdHash = hash(user.password)
    val query = sql"insert into paint_fun.users (login, name, password_hash) " ++
      sql"values(${user.login}, ${user.name}, $pwdHash) on conflict do nothing"
    val res = query.update.run.map {
      case 0 => UserValidation.loginAlreadyExists
      case _ => user.validNel
    }
    res.transact(xa)
  }

  def find(login: String): F[Option[User]] = {
    sql"select login, name from paint_fun.users where login = $login".query[(String, String)].map {
      case (login, name) => User(login, name, "")
    }.option.transact(xa)
  }

  def verifyCredentials(user: User): F[AllErrorsOr[User]] = validateCredentials(user) match {
    case Valid(_) =>
      val query = sql"select password_hash from paint_fun.users where login = ${user.login}".query[String].option
      OptionT(query.transact(xa))
        .map(checkPassword(user.password, _))
        .filter(_ == true)
        .fold(credentialsNotValid)(_ => user.validNel)

    case Invalid(e) => e.invalid[User].pure[F]
  }
}

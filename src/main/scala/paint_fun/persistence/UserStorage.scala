package paint_fun.persistence

import cats.data.{EitherT, OptionT}
import cats.effect.{Async, ContextShift, IO}
import cats.implicits._
import doobie.ConnectionIO
import doobie.implicits._
import org.slf4j.{Logger, LoggerFactory}
import paint_fun.model.ValidationErrors.AllErrorsOr
import paint_fun.model._
import paint_fun.routes.Auth

trait UserStorage[F[_]] {
  def save(user: User): F[AllErrorsOr[User]]
  def find(login: String): F[Option[User]]
  def verify(user: User): F[Boolean]
  def all(): F[List[User]]
}

object UserStorage {
  def apply[F[_] : UserStorage]: UserStorage[F] = implicitly

  def instance[F[_] : Async : ContextShift]: UserStorage[F] = new UserStorageImpl[F]
}

class UserStorageImpl[F[_]](implicit
                            async: Async[F],
                            cs: ContextShift[F]
                           ) extends DbConnection[F] with UserStorage[F] {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  def save(user: User): F[AllErrorsOr[User]] = {
    val pwdHash = Auth.hash(user.password)
    val unique = sql"select 1 from paint_fun.users where login = ${user.login}".query[Int].option.map {
      case Some(_) => None
      case None => Some(1)
    }
    val insert = sql"insert into paint_fun.users (login, name, password_hash) values(${user.login}, ${user.name}, $pwdHash)".update.run.as(user)

    val query = OptionT(unique).semiflatMap(_ => insert).value
    val alreadyExists = ValidationErrors.AlreadyExists(UserValidation.Login)
    val z = EitherT.fromOptionF[ConnectionIO, ValidationError, User](query, alreadyExists)

    val vld = EitherT.fromEither[F](UserValidation.validate(user).toEither)
    val exec = () => transactor.use(xa => z.transact(xa).toValidatedNel).map(_.toEither)

    vld.flatMapF(_ => exec()).value.map(_.toValidated)
  }

  def find(login: String): F[Option[User]] = transact {
    sql"select * from paint_fun.users where login = $login".query[User].option
  }

  def all(): F[List[User]] = transact {
    sql"select * from paint_fun.users".query[User].to[List]
  }

  def verify(user: User): F[Boolean] = OptionT(transact {
    sql"select password_hash from paint_fun.users where login = ${user.login}".query[String].option
  }).map(Auth.checkPassword(user.password, _)).getOrElse(false)
}

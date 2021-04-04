package paint_fun.routes

import cats.effect.{Blocker, ContextShift, Sync}
import org.http4s.HttpRoutes
import org.http4s.server.staticcontent.{ResourceService, resourceService}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object StaticRoutes {
  def assets[F[_] : Sync : ContextShift]: HttpRoutes[F] = {
    val threadPool = Executors.newCachedThreadPool()
    val ec = ExecutionContext.fromExecutorService(threadPool)
    val blocker = Blocker.liftExecutionContext(ec)
    resourceService[F](ResourceService.Config("/assets", blocker))
  }
}

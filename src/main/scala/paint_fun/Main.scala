package paint_fun

import cats.effect.{ExitCode, IO, IOApp}
import paint_fun.server.PaintFunServer

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    PaintFunServer.run[IO].use(_.compile.lastOrError)
  }
}

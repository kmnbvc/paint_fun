package paint_fun

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]) =
    Paintfunhttp4sServer.stream[IO].compile.drain.as(ExitCode.Success)
}

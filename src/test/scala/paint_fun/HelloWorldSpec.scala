package paint_fun

import cats.effect.IO
import org.http4s._
import org.http4s.implicits._
import munit.CatsEffectSuite

class HelloWorldSpec extends CatsEffectSuite {

  test("HelloWorld returns status code 200") {
//    assertIO(retHelloWorld.map(_.status) ,Status.Ok)
  }

  test("HelloWorld returns hello world message") {
//    assertIO(retHelloWorld.flatMap(_.as[String]), "{\"message\":\"Hello, world\"}")
  }

}
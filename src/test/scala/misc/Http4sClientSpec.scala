package misc

import cats.effect._
import cats.effect.std.Random
import cats.implicits._
import org.http4s.blaze.client._
import weaver._

object Http4sClientSpec extends SimpleIOSuite {

  test("should make call") {
    val program = Random.scalaUtilRandom[IO].flatMap { random =>
        BlazeClientBuilder[IO].resource.use { httpClient =>
          for {
            int <- random.nextInt
            id = Math.abs(int) % 100
            k <- httpClient.expect[String](s"http://numbersapi.com/$id/trivia")
          } yield k
        }
      } >>= IO.println

    program.map(_Passed)
  }

}

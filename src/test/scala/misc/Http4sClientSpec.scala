package misc

import cats.effect._
import cats.implicits._
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.http4s.blaze.client._
import org.http4s.client.Client
import weaver._

import scala.concurrent.duration._

object Http4sClientSpec extends IOSuite {

  def wireMockServerResource[F[_]: Sync](
      stubbing: WireMockServer => F[Unit]
  ): Resource[F, WireMockServer] =
    Resource.make[F, WireMockServer](
      Sync[F]
        .delay {
          val server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(0))
          server.start()
          server
        }
        .flatTap(stubbing)
    )(server => Sync[F].delay(server.stop()))

  case class Setup[F[_]](wireMockServer: WireMockServer, httpClient: Client[F])
  override type Res = Setup[IO]
  override def sharedResource: Resource[IO, Res] =
    for {
      wiremock           <- wireMockServerResource[IO](_ => IO.unit)
      client: Client[IO] <- BlazeClientBuilder[IO].resource
    } yield Setup(wiremock, client)

  test("should make the call") { httpSetup =>
    import httpSetup._
    wireMockServer.stubFor(WireMock.get("/").willReturn(WireMock.ok("done").withFixedDelay(100)))
    val program = httpClient
        .expect[String](s"http://localhost:${wireMockServer.port()}/")
        .map(string => assert(string == "done"))
        .timeout(500.milliseconds)
        .timed >>= IO.print
    program.map(_Passed)
  }

}

package misc

import cats.effect.{ IO, Resource }
import weaver.IOSuite

import java.util.concurrent.ConcurrentLinkedQueue

object ResourceDemo extends IOSuite {

  val order = new ConcurrentLinkedQueue[String]()

  def record(msg: String): IO[Unit] = IO(order.add(msg)).void

  override type Res = Int
  override def sharedResource: Resource[IO, Int] = {
    val acquire = record("Acquiring resource") *> IO.pure(42)
    val release = (i: Int) => record(s"Releasing resource $i")
    Resource.make(acquire)(release)
  }

  test("Test 1") { res =>
    record(s"Test 1 is using resource $res").as(success)
  }

  test("Test 2") { res =>
    record(s"Test 2 is using resource $res").as(expect(res == 45))
  }
}

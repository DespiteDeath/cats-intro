package example

import scala.concurrent._, scala.util._
import cats._, cats.data._, cats.implicits._, cats.effect._

object EffectsMain extends App {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  val start = System.currentTimeMillis()
  val one = IO[Int] {
    Thread.sleep(2000L)
    2
  }
  val two = IO[Int] {
    Thread.sleep(3000L)
    3
  }

  val m = IO((x: Int, y: Int) => x + y)
  val t = (one, two).parMapN {
    case (m, n) => m + n
  }

  t.unsafeRunAsync {
    case Left(x) =>
      x.printStackTrace()
    case Right(x) =>
      println(s"${System.currentTimeMillis() - start} finished $x")
  }

  Thread.sleep(6000L)

}

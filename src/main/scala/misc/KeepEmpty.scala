package misc

import cats._
import cats.implicits._
import cats.data._
import cats.effect._
import cats.effect.implicits._

import scala.concurrent.duration._
import scala.util.control.NoStackTrace

object main41 extends IOApp.Simple {

  override def run: IO[Unit] =
    for {
      printLoop1 <- (IO.println("Hello") >> IO.sleep(100 millis)).foreverM.start
      printLoop2 <- (IO.println("world") >> IO.sleep(150 millis)).foreverM.start
      _          <- IO.sleep(3 seconds) >> printLoop1.cancel >> printLoop2.cancel
    } yield ExitCode.Success
}

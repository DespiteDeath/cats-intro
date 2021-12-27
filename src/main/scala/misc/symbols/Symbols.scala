package misc.symbols

import cats.effect._
import cats.implicits._

import scala.util.control.NoStackTrace

object main1 extends IOApp.Simple {
  case object error1 extends RuntimeException with NoStackTrace
  case object error2 extends RuntimeException with NoStackTrace
  val io1: IO[Int]     = IO("part1").flatTap(IO.println) >> IO(2).flatTap(IO.println)
  val io2: IO[Nothing] = IO("part1").flatTap(IO.println) >> IO.raiseError(error2)
  val io3: IO[Int]     = IO.raiseError(error1) >> IO(2).flatTap(IO.println)
  val io4: IO[Nothing] = IO.raiseError(error1) >> IO.raiseError(error2)

  override def run: IO[Unit] = io4.as(())
}

object main2 extends IOApp.Simple {
  case object error1 extends RuntimeException with NoStackTrace
  case object error2 extends RuntimeException with NoStackTrace
  val io1: IO[Int]     = IO("part1").flatTap(IO.println) *> IO(2).flatTap(IO.println)
  val io2: IO[Nothing] = IO("part1").flatTap(IO.println) *> IO.raiseError(error2)
  val io3: IO[Int]     = IO.raiseError(error1) *> IO(2).flatTap(IO.println)
  val io4: IO[Nothing] = IO.raiseError(error1) *> IO.raiseError(error2)

  override def run: IO[Unit] = io1.as(())
}

object main3 extends IOApp.Simple {
  case object error1 extends RuntimeException with NoStackTrace
  case object error2 extends RuntimeException with NoStackTrace
  val io1: IO[String]  = IO("part1").flatTap(IO.println) <* IO(2).flatTap(IO.println)
  val io2: IO[String]  = IO("part1").flatTap(IO.println) <* IO.raiseError(error2)
  val io3: IO[Nothing] = IO.raiseError(error1) <* IO(2).flatTap(IO.println)
  val io4: IO[Nothing] = IO.raiseError(error1) <* IO.raiseError(error2)

  override def run: IO[Unit] = io4.as(())
}

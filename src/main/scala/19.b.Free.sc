//https://blog.scalac.io/2016/06/02/overview-of-free-monad-in-cats.html?

import cats._
import cats.free._

case class Degree(private val d: Int) {
  val value = d % 360
}
case class Position(x: Double, y: Double, heading: Degree)

sealed trait Instruction[A]

case class Forward(position: Position, length: Int) extends Instruction[Position]
case class Backward(position: Position, length: Int) extends Instruction[Position]
case class RotateLeft(position: Position, degree: Degree) extends Instruction[Position]
case class RotateRight(position: Position, degree: Degree) extends Instruction[Position]
case class ShowPosition(position: Position) extends Instruction[Unit]

object Instructions {
  def forward(pos: Position, l: Int): Free[Instruction, Position]  = Free.liftF(Forward(pos, l))
  def backward(pos: Position, l: Int): Free[Instruction, Position] = Free.liftF(Backward(pos, l))
  def left(pos: Position, degree: Degree): Free[Instruction, Position] =
    Free.liftF(RotateLeft(pos, degree))
  def right(pos: Position, degree: Degree): Free[Instruction, Position] =
    Free.liftF(RotateRight(pos, degree))
  def showPosition(pos: Position): Free[Instruction, Unit] = Free.liftF(ShowPosition(pos))
}

object Computations {
  def forward(pos: Position, l: Int): Position =
    pos.copy(
      x = pos.x + l * math.cos(pos.heading.value * math.Pi / 180.0),
      y = pos.y + l * math.sin(pos.heading.value * math.Pi / 180.0)
    )

  def backward(pos: Position, l: Int): Position =
    pos.copy(
      x = pos.x - l * math.cos(pos.heading.value * math.Pi / 180.0),
      y = pos.y - l * math.sin(pos.heading.value * math.Pi / 180.0)
    )

  def left(pos: Position, d: Degree): Position =
    pos.copy(heading = Degree(pos.heading.value + d.value))

  def right(pos: Position, d: Degree): Position =
    pos.copy(heading = Degree(pos.heading.value - d.value))
}

object InterpreterId extends (Instruction ~> Id) {

  import Computations._

  override def apply[A](fa: Instruction[A]): Id[A] =
    fa match {
      case Forward(p, length)     => forward(p, length)
      case Backward(p, length)    => backward(p, length)
      case RotateLeft(p, degree)  => left(p, degree)
      case RotateRight(p, degree) => right(p, degree)
      case ShowPosition(p)        => println(s"showing position $p")
    }
}

object InterpreterOption extends (Instruction ~> Option) {

  import Computations._

  val nonNegative: (Position) => Option[Position] = { p =>
    if (p.x >= 0 && p.y >= 0) Some(p) else None
  }

  override def apply[A](fa: Instruction[A]) =
    fa match {
      case Forward(p, length)     => nonNegative(forward(p, length))
      case Backward(p, length)    => nonNegative(backward(p, length))
      case RotateLeft(p, degree)  => Some(left(p, degree))
      case RotateRight(p, degree) => Some(right(p, degree))
      case ShowPosition(p)        => Some(println(s"showing position $p"))
    }
}

object InterpreterFuture extends (Instruction ~> scala.concurrent.Future) {

  import Computations._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Future

  override def apply[A](fa: Instruction[A]) =
    fa match {
      case Forward(p, length)     => Future(forward(p, length))
      case Backward(p, length)    => Future(backward(p, length))
      case RotateLeft(p, degree)  => Future(left(p, degree))
      case RotateRight(p, degree) => Future(right(p, degree))
      case ShowPosition(p)        => Future(println(s"showing position $p"))
    }
}

sealed trait PencilInstruction[A]
case class PencilUp(position: Position) extends PencilInstruction[Unit]
case class PencilDown(position: Position) extends PencilInstruction[Unit]
//type LogoApp[A] = Coproduct[Instruction, PencilInstruction, A]

val startPosition = Position(0.0, 0.0, Degree(0))

val program: (Position => Free[Instruction, Position]) = { start: Position =>
  import Instructions._
  for {
    p1 <- forward(start, 10)
    p2 <- right(p1, Degree(90))
    p3 <- forward(p2, 10)
  } yield p3
}

val program2: (Position => Free[Instruction, Unit]) = { s: Position =>
  import Instructions._
  for {
    p1 <- forward(s, 10)
    p2 <- right(p1, Degree(90))
    p3 <- forward(p2, 10)
    p4 <- backward(p3, 20) //Here the computation stops, because result will be None
    _  <- showPosition(p4)
  } yield ()
}

program(startPosition).foldMap(InterpreterId)
//program(startPosition).foldMap(InterpreterOption)
//program(startPosition).foldMap(InterpreterFuture)
//program2(startPosition).foldMap(InterpreterOption)

import cats.data._

import scala.concurrent._
implicit val cte = ExecutionContext.fromExecutor(_.run())

case class Generator(seed: Int)

object Generator {
  import State._
  def generateRandomInt(): State[Generator, Int] =
    for {
      g <- get[Generator] // Get the current state
      _ <- set(Generator(g.seed + 1)) // Update the state with the new generator
    } yield g.seed + 5 // return the "randomly" generated number
  def generateRandomIntV2(): State[Generator, Int] =
    State { s =>
      (Generator(s.seed + 1), s.seed + 5)
    }
}
import Generator._

val computation = for {
  x <- generateRandomInt()
  y <- generateRandomInt()
} yield (x, y)

computation.run(Generator(57)).value

val computation2 = for {
  x <- generateRandomIntV2()
  y <- generateRandomIntV2()
} yield (x, y)

computation2.run(Generator(57)).value

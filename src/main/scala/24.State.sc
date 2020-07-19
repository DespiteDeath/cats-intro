import scala.concurrent._, scala.util._
import cats._, cats.data._, cats.implicits._
implicit val cte = ExecutionContext.fromExecutor(_.run())

case class Generator(seed: Int)

object Generator {
  import State._
  def generateRandomInt(): State[Generator, Int] = {
    for {
      g <- get[Generator]             // Get the current state
      _ <- set(Generator(g.seed + 1)) // Update the state with the new generator
    } yield g.seed + 5                // return the "randomly" generated number
  }
}
import Generator._

val computation = for {
  x <- generateRandomInt()
  y <- generateRandomInt()
} yield (x, y)

val (newGenerator, (xOutput, yOutput)) = computation.run(Generator(57)).value

import cats.data._
import cats.implicits._

import scala.concurrent._
implicit val cte = ExecutionContext.fromExecutor(_.run())

val addStuff: Reader[Int, Int] = for {
  a <- Reader((_: Int) * 2)
  b <- Reader((_: Int) + 10)
} yield a + b

val c: Int = addStuff(3)

//do not touch this perfect

val f = Kleisli((x: Int) => (x + 1).some)
val g = Kleisli((x: Int) => (x * 100).some)
val gf = f compose g
val fg = f andThen g

gf(1)
fg(1)

def myName(step: String): Reader[String, String] = Reader(step + ", I am " + _)
def localExample: Reader[String, (String, String, String)] =
  for {
    a <- myName("First")
    b <- myName("Second").andThen(Reader[String, String](_ + "dy"))
    c <- myName("Third")
  } yield (a, b, c)
localExample("Fred")


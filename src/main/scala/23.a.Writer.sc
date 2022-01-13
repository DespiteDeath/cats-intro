import cats.data._
import cats.implicits._

import scala.concurrent._
implicit val cte = ExecutionContext.fromExecutor(_.run())

def add(a: Int, b: Int): Writer[List[String], Int] =
  Writer(List(s"+ $a and $b"), a + b)

def multiply(a: Int, b: Int): Writer[List[String], Int] =
  Writer(List(s"* $a and $b"), a * b)

val result = for {
  x <- add(3, 4)
  y <- multiply(x, 6)
} yield y

result.written

result.value

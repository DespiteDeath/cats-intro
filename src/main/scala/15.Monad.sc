import cats._
import cats.implicits._

import scala.concurrent._

implicit val cte = ExecutionContext.fromExecutor(_.run())

def myLength[M[_]: Monad](ma: M[String], mb: M[String]): M[Int] =
    for {
      a <- ma
      b <- mb
    } yield a.length + b.length

myLength(List("mert", "inan"), List("mert", "inan"))
myLength(Option("mert"), Option("inan"))
myLength(Future("hello"), Future("mert"))

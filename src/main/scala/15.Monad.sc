import scala.concurrent._, scala.util._
import cats._, cats.data._, cats.implicits._

implicit val cte = ExecutionContext.fromExecutor(_.run())

def myLength[M[_] : Monad](ma: M[String], mb: M[String]): M[Int] =
  for {
    a <- ma
    b <- mb
  } yield a.length + b.length


myLength(List("mert", "inan"),List("mert", "inan"))
myLength(Option("mert"), Option("inan"))
myLength(Future("hello"), Future("mert"))

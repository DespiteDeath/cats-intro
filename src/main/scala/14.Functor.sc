import scala.concurrent._, scala.util._
import cats._, cats.data._, cats.implicits._
implicit val cte = ExecutionContext.fromExecutor(_.run())

Functor[Option].map(Option("mert"))(_.length)
Functor[List].map(List("mert", "inan"))(_.length)

def myLength[F[_] : Functor](fa: F[String]): F[Int] =
  Functor[F].map(fa)(_.length)


myLength(List("mert", "inan"))
myLength(Option("mert"))
myLength(Future("hello"))

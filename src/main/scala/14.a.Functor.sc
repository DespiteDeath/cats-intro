import cats._
import cats.data._
import cats.implicits._

import scala.concurrent._
import scala.util._

implicit val cte: ExecutionContextExecutor = ExecutionContext.fromExecutor(_.run())

Functor[Option].map(Option("mert"))(_.length)
Functor[List].map(List("mert", "inan"))(_.length)

val len: String => Int = _.length

//map
def myLength[F[_]: Functor](fa: F[String]): F[Int] =
  Functor[F].map(fa)(len)

myLength(List("mert", "inan"))
myLength(Option("mert"))
myLength(Future("hello"))

//lift
val myLength1: List[String] => List[Int] = Functor[List].lift(len)
myLength1(List("mert", "inan"))

//fproduct
Functor[List].fproduct(List("mert", "inan"))(len).toMap
Functor[Option].fproduct(Some("123"))(len)

//compose
val listOps = Functor[List] compose Functor[Option]
listOps.map(List(Some(1), None, Some(3)))(_ + 1)

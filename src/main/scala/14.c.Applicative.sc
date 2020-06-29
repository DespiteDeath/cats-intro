import scala.concurrent._, scala.util._
import cats._, cats.data._, cats.implicits._
implicit val cte = ExecutionContext.fromExecutor(_.run())


//Adds a pure method to Apply:
Applicative[Option].pure(1)
Applicative[List].pure(1)
(Applicative[List] compose Applicative[Option]).pure(1)

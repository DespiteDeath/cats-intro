import cats._
import cats.implicits._

import scala.concurrent._
implicit val cte = ExecutionContext.fromExecutor(_.run())

//Adds a pure method to Apply:
Applicative[Option].pure(1)
Applicative[List].pure(1)
(Applicative[List] compose Applicative[Option]).pure(1)

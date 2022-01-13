import cats._
import cats.implicits._

import scala.concurrent._
implicit val cte = ExecutionContext.fromExecutor(_.run())

//Functor map: A → B
//Apply ap: F[A → B]

val intToString: Int => String = _.toString
val double: Int => Int = _ * 2
val addTwo: Int => Int = _ + 2
Apply[Option].ap(Some(intToString))(Some(1))
Apply[Option].ap(Some(double))(Some(1))
Apply[Option].ap(None)(Some(double))
Apply[Option].ap(None)(Some(1))
Apply[Option].ap(None)(None)

//ap
Option((_: Int) + 1) <*> Option(11)
Option((_: Int) + 1).ap(Option(11))
Option((_: Int) + (_: Int)).ap2(Option(1), Option(2))

//map
Apply[Option].map(Some(1))(double)
Apply[Option].map2(Some(1), Some(3))(_ * _)

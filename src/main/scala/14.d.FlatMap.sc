import cats._
import cats.implicits._

import scala.concurrent._
implicit val cte = ExecutionContext.fromExecutor(_.run())

//Adds a pure method to Apply:
FlatMap[Option].flatMap(Some(1))(x => Some(x + 1))

import scala.concurrent._, scala.util._
import cats._, cats.data._, cats.implicits._
implicit val cte = ExecutionContext.fromExecutor(_.run())


Monoid[String].empty
Monoid[String].combineAll(List("a","b","c"))
Monoid[String].combineAll(List())

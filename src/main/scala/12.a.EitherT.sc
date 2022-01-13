import cats.data._
import cats.implicits._

import scala.concurrent._
import scala.util._

implicit val cte: ExecutionContextExecutor = ExecutionContext.fromExecutor(_.run())

type FEI = Future[String Either Int]

def reduce(test: String, x: FEI, y: FEI): Unit = {
    val z: EitherT[Future, String, Int] = for {
      x <- EitherT(x)
      y <- EitherT(y)
    } yield x + y
    z.value onComplete {
      case Success(Right(value)) => println(s"r : $test : $value")
      case Success(Left(value))  => println(s"l :$test $value")
      case Failure(_)            => println(s"$test Exception")
    }
  }

val ex = new RuntimeException
reduce("test0", Future(Right(3)), Future(Right(2)))
reduce("test1", Future(Right(1)), Future(Left("xleft")))
reduce("test2", Future(Left("xleft")), Future(Right(2)))
reduce("test3", Future(Left("xleft")), Future.failed(ex))
reduce("test4", Future.failed(ex), Future(Left("xleft")))
reduce("test5", Future.failed(ex), Future.failed(ex))



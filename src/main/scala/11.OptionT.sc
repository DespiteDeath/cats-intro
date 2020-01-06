import scala.concurrent._, scala.util._
import cats.data._, cats.implicits._

implicit val cte = ExecutionContext.fromExecutor(_.run())

type FOI = Future[Option[Int]]

def reduce(test: String, x: FOI, y: FOI): Unit = {
    val z: OptionT[Future, Int] = for {
      x <- OptionT(x)
      y <- OptionT(y)
    } yield x + y
    z.value onComplete {
      case Success(Some(r)) => println(s"$test : $r")
      case Success(None)    => println(s"$test None")
      case Failure(_)       => println(s"$test Exception")
    }
  }

val ex = new RuntimeException
reduce("test0", Future(Some(1)), Future(Some(2)))
reduce("test1", Future(Some(1)), Future(None))
reduce("test2", Future(None), Future(Some(2)))
reduce("test3", Future(None), Future.failed(ex))
reduce("test4", Future.failed(ex), Future(None))
reduce("test5", Future.failed(ex), Future.failed(ex))



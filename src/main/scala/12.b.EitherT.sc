import cats.data._
import cats.implicits._

import scala.concurrent._
import scala.util._

implicit val cte: ExecutionContextExecutor = ExecutionContext.fromExecutor(_.run())

def f(i: Int): Future[Int] =
  Future {
    if (i % 2 == 0) throw new RuntimeException(i.toString)
    else i
  }

val fs = Iterator.range(0, 10).map(f)

fs.foldLeft(Future.successful((List.empty[Throwable], List.empty[Int]))) { (acc, elem) =>
  val m = for {
    _elem  <- elem
    (f, s) <- acc
  } yield (f, s :+ _elem)
  m.fallbackTo {
    for {
      _elem  <- elem.failed
      (f, s) <- acc
    } yield (f :+ _elem, s)
  }
}

def ei(i: Int): Future[Either[Throwable, Int]] =
  Future {
    if (i == 0) throw new RuntimeException(i.toString)
    if (i % 2 == 0) Left(new RuntimeException(i.toString))
    else Right(i)
  }

val fs1 = Range(0, 10).map(ei)
type ResultE = Either[Any, (List[Throwable], List[Int])]
fs1.foldLeft[Future[ResultE]](Future.successful(Right((List.empty[Throwable], List.empty[Int]))))(
  op = (acc, elem) => {

    val s1 = for {
      _elem <- EitherT(elem).leftWiden[Any]
      r     <- EitherT(acc)
    } yield {
      val (f, s) = r
      (f, s :+ _elem)
    }

    val s2 = s1.orElse {
      for {
        _elem <- EitherT(elem.map(_.swap)).leftWiden[Any]
        r     <- EitherT(acc)
      } yield {
        val (f, s) = r
        (f :+ _elem, s)
      }
    }

    val s3 = s2.value.fallbackTo {
      for {
        _elem         <- elem.failed
        Right((f, s)) <- acc
      } yield Right((f :+ _elem, s))
    }
    s3
  }
)



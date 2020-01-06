package example

import scala.concurrent._, duration._
import cats._, implicits._, effect._, concurrent._, data._

object EffectsMain extends IOApp {

  val compute: IO[Long] = {
    def makeIO(r: Long): IO[Long] = IO.sleep(r seconds).flatMap(_ => IO.pure(r))

    val two   = makeIO(2)
    val three = makeIO(3)

    (two, three).parMapN {
      case (m, n) => m + n
    }

  }

  override def run(args: List[String]): IO[ExitCode] =
    now
      .bracket(_ => compute)(start =>
        now.map(_ - start).flatMap(duration => putStrLine[IO](duration.toString))
      )
      .handleErrorWith(e => IO(e.printStackTrace()))
      .as(ExitCode.Success)
}

object ParMapN extends IOApp {
  val ioA = IO.sleep(10.seconds) *> IO(println("Delayed!"))
  val ioB = IO.raiseError[Unit](new Exception("dummy"))
  override def run(args: List[String]): IO[ExitCode] =
    (ioA, ioB).parMapN((_, _) => ()).as(ExitCode.Success)
}

object ParTraverse extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    NonEmptyList
      .of(1, 2, 3)
      .parTraverse(i => putStrLine[IO](i.toString))
      .as(ExitCode.Success)
}

object ParFailFast extends IOApp {

  def handleError(s: String, th: Throwable): IO[String] = {
    th.printStackTrace()
    IO.pure(s"$s failed")
  }

  def makeIO(s: String, d: FiniteDuration): IO[String] =
    putStrLine[IO](s"start io$s") *>
    IO {
      if (scala.util.Random.nextBoolean()) throw new IllegalArgumentException(s"$s failed")
      s
    } <* IO.sleep(d) <* putStrLine[IO](s"done io$s")

  val a: IO[String] = makeIO("a", 1 second)

  val b: IO[String] = makeIO("b", 5 second)

  val c: IO[String] = makeIO("c", 3 second)

  def run1(args: List[String]): IO[ExitCode] =
    Parallel
      .parMap3(a, b, c) {
        case (m, n, o) => s"$m - $n - $o"
      }
      .map(println)
      .as(ExitCode.Success)

  val handler: PartialFunction[Throwable, IO[String]] = {
    case e => putStrLine[IO](s"parFailFast-handler: $e") *> IO.raiseError(e)
  }

  val logic: IO[Either[Throwable, List[Either[Throwable, String]]]] = List(a, b, c).parTraverse {
    fa =>
      Deferred[IO, Either[Throwable, String]].flatMap { d =>
        Concurrent[IO].start(fa.recoverWith(handler).attempt.flatMap(d.complete)) *> d.get
      }
  }.attempt

  def run(args: List[String]): IO[ExitCode] =
    now
      .bracket(_ => logic)(start => now.map(_ - start).map(println))
      .as(ExitCode.Success)

}

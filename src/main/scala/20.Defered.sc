import cats.effect.IO
import cats.effect.concurrent.Deferred
import cats.implicits._
import scala.concurrent.ExecutionContext

implicit val cs = IO.contextShift(ExecutionContext.global)

def start(d: Deferred[IO, Int]): IO[Unit] = {
    val attemptCompletion: Int => IO[Unit] = n => d.complete(n).attempt.void

    List(
      //both try to complete
      IO.race(attemptCompletion(1), attemptCompletion(2)),
      d.get.flatMap(n => IO(println(show"Result: $n")))
    ).parSequence.void
  }

val program: IO[Unit] =
    for {
      d <- Deferred[IO, Int]
      _ <- start(d)
    } yield ()

program.unsafeRunSync()

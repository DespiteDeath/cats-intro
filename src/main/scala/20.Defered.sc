import cats.effect.IO
import cats.effect.kernel.Deferred
import cats.effect.unsafe.implicits.global
import cats.implicits._

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

import cats.Applicative
import cats.implicits._
import cats.effect._
import cats.effect.unsafe.implicits.global
import fs2._

Stream(1, 2, 3)

Chunk.seq(Seq(1, 2, 3))

Stream.emits(List(1, 2, 3))

val m1 = Stream.eval(IO(println("ok1"))).compile.drain.unsafeRunSync()

val m2 = Stream.evals(IO(println("ok1")).replicateA(10)).compile.drain.unsafeRunSync()

Stream((1 to 100): _*)
  .chunkN(10) // group 10 elements together
  .map(println)
  .compile
  .drain

1.some
1.asRight[Throwable]

import fs2._
import cats.effect._

Stream(1, 2, 3)

Chunk.seq(Seq(1, 2, 3))

Stream.emits(List(1, 2, 3))

Stream.eval_(IO(println("ok1"))).compile.drain.unsafeRunSync()




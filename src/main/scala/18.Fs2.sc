import fs2._
import cats.effect._
import cats.effect.unsafe.implicits.global

Stream(1, 2, 3)

Chunk.seq(Seq(1, 2, 3))

Stream.emits(List(1, 2, 3))

Stream.eval(IO(println("ok1"))).compile.drain.unsafeRunSync()




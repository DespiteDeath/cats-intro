package misc

import cats._
import cats.effect._
import cats.implicits._

trait Counter[F[_]] {
  def incr: F[Unit]
  def get: F[Int]
}

object Counter {
  def make[F[_]: Functor: Ref.Make]: F[Counter[F]] =
    Ref.of[F, Int](0).map { ref =>
      new Counter[F] {
        def incr: F[Unit] = ref.update(_ + 1)
        def get: F[Int]   = ref.get
      }
    }
}

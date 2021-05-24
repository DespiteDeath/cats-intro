package example

object ForComprehension {

  def main(args: Array[String]): Unit = {
    import cats._
    import cats.effect._
    import cats.implicits._
    import unsafe.implicits.global
    def `for`[F[_]: Sync]: F[Unit] =
      for {
        str <- getStrLine()
        _ <- Monad[F].pure(2) //no reason
        _   <- putStrLine(str)
      } yield ()

    `for`[IO].unsafeRunSync()

    def symbol[F[_]: Sync]: F[Unit] =
      getStrLine[F]() >>= putStrLine[F]
    symbol[IO].unsafeRunSync()

  }
}

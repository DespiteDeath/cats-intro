import cats.effect._

package object example {

  def putStrLine[F[_]: Sync](str: String): F[Unit] = Sync[F].delay(println(str))
  def getStrLine[F[_]: Sync](): F[String]          = Sync[F].delay(scala.io.StdIn.readLine())
  val now: IO[Long]                                = IO[Long](System.currentTimeMillis())

}

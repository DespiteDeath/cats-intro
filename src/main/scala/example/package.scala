import cats.effect._
import cats.effect.std.Console
import cats.effect.unsafe.{ IORuntime, IORuntimeConfig, Scheduler }

import java.util.concurrent.ScheduledThreadPoolExecutor

package object example {

  def putStrLine[F[_]: Sync](str: String): F[Unit] = Sync[F].delay(println(str))
  def getStrLine[F[_]: Sync](): F[String]          = Sync[F].delay(scala.io.StdIn.readLine())
  val now: IO[Long]                                = IO[Long](System.currentTimeMillis())

  def unsafePrintCurrentThread(message: String = ""): Unit =
    if (message.isBlank) println(s"${Thread.currentThread().getName}")
    else println(s"${Thread.currentThread().getName}: $message")

  def printCurrentThreadIO(message: String = ""): IO[Unit] =
    if (message.isBlank) IO.println(s"${Thread.currentThread().getName}")
    else IO.println(s"${Thread.currentThread().getName}: $message")

  def _printCurrentThreadIO(ignoring: Any = ""): IO[Unit] =
    IO.println(s"${Thread.currentThread().getName}")

  def printCurrentThread[F[_]: Console](message: String = ""): F[Unit] =
    if (message.isBlank) Console[F].println(s"${Thread.currentThread().getName}")
    else Console[F].println(s"${Thread.currentThread().getName}: $message")

  def _printCurrentThread[F[_]: Console](ignoring: Any = ""): F[Unit] =
    Console[F].println(s"${Thread.currentThread().getName}")

  val scheduler = new ScheduledThreadPoolExecutor(
    1,
    { r: Runnable =>
      val t = new Thread(r: Runnable)
      t.setName("fcuk")
      t.setDaemon(true)
      t.setPriority(Thread.MAX_PRIORITY)
      t
    }
  )
  val MyIORuntime =
    IORuntime(
      scala.concurrent.ExecutionContext.global,
      scala.concurrent.ExecutionContext.global,
      Scheduler.fromScheduledExecutor(scheduler),
      () => (),
      IORuntimeConfig()
    )

}

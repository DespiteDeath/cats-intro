import cats.effect._
import cats.effect.unsafe.{ IORuntime, IORuntimeConfig, Scheduler }

import java.util.concurrent.ScheduledThreadPoolExecutor

package object example {

  def putStrLine[F[_]: Sync](str: String): F[Unit] = Sync[F].delay(println(str))
  def getStrLine[F[_]: Sync](): F[String]          = Sync[F].delay(scala.io.StdIn.readLine())
  val now: IO[Long]                                = IO[Long](System.currentTimeMillis())

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

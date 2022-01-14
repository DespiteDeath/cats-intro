import cats.effect._
import weaver.Expectations

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, TimeoutException }
import scala.util.control.NoStackTrace

package object misc extends Expectations.Helpers {
  def e1: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(runnable => new Thread(runnable, "th1").start())
  def e2: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(runnable => new Thread(runnable, "th2").start())
  def e3: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(runnable => new Thread(runnable, "th3").start())
  def e4: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(runnable => new Thread(runnable, "th4").start())
  def e5: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(runnable => new Thread(runnable, "th5").start())
  def e6: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(runnable => new Thread(runnable, "th6").start())

  def printCurrentThread(): Unit               = println(Thread.currentThread().getName)
  def printCurrentThreadIO: IO[Unit]           = IO(printCurrentThread())
  def _printCurrentThreadIO[A](a: A): IO[Unit] = IO(printCurrentThread())

  def _Passed[A](a: A): Expectations = assert(true)
  val Passed: Expectations           = assert(true)

  def _Failed[A](a: A): Expectations = assert(false)
  val Failed: Expectations           = assert(false)

  case object Timeout extends TimeoutException with NoStackTrace
}

import cats.Functor
import cats.effect._
import cats.effect.std.Console
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

  def _Passed[A](a: A): Expectations = assert(true)
  val Passed: Expectations           = assert(true)

  def _Failed[A](a: A): Expectations = assert(false)
  val Failed: Expectations           = assert(false)

  case object Timeout extends TimeoutException with NoStackTrace

  implicit def toExpectation[F[_]: Functor](k: F[Unit]): F[Expectations] =
    Functor[F].map(k)(_Passed)
}

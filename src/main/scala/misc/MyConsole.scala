package misc

import cats._
import cats.data._
import cats.effect._
import cats.implicits._

trait MyConsole[F[_]] {
  def printLn(string: String): F[Unit]
  def readLn(): F[String]
}

object MyConsole {
  def apply[F[_]](implicit ev: MyConsole[F]): MyConsole[F] = ev

  object state {
    type ConsoleState[A] = State[List[String], A]

    implicit object MyConsoleState extends MyConsole[ConsoleState] {
      override def printLn(string: String): ConsoleState[Unit] =
        State.modify(_ :+ string)

      override def readLn(): ConsoleState[String] =
        State.pure("test")
    }
  }

  object ref {
    type MyRef[F[_]] = Ref[F, List[String]]

    class ConsoleRef[F[_]: Applicative](val ref: MyRef[F]) extends MyConsole[F] {
      override def printLn(string: String): F[Unit] = ref.update(_ :+ string)
      override def readLn(): F[String]              = Applicative[F].pure("test")
    }

    object ConsoleRef {
      def make[F[_]: Applicative: Ref.Make]: F[ConsoleRef[F]] =
        Ref.of[F, List[String]](List.empty).map(ref => new ConsoleRef(ref))
    }
  }

  object sync {
    class ConsoleSync[F[_]: Sync] extends MyConsole[F] {
      override def printLn(string: String): F[Unit] = Sync[F].delay(println(string))
      override def readLn(): F[String]              = Sync[F].delay(scala.io.StdIn.readLine())
    }
  }

  def readUserName[F[_]: Monad: MyConsole](): F[Unit] =
    for {
      _    <- MyConsole[F].printLn("Enter your name")
      name <- MyConsole[F].readLn()
      _    <- MyConsole[F].printLn(s"Your name's $name")
    } yield ()
}

object Program extends App {
  import cats.effect.unsafe.implicits.global
  import misc.MyConsole.sync.ConsoleSync
  implicit val console: ConsoleSync[IO] = new ConsoleSync[IO]
  MyConsole.readUserName[IO]().unsafeRunSync()
}

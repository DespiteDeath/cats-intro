package example

import cats.effect._
import cats.effect.implicits._
import cats.implicits._

import java.io._
import java.net._
import scala.util.Try

object ClientServer extends IOApp.Simple {

  def echoProtocol[F[_]: Sync](clientSocket: Socket): F[Unit] = {

    def loop(reader: BufferedReader, writer: BufferedWriter): F[Unit] =
      for {
        line <- Sync[F].delay(reader.readLine())
        _ <- line match {
          case "" => Sync[F].unit // Empty line, we are done
          case _ =>
            Sync[F].delay { writer.write(line); writer.newLine(); writer.flush() } >>
            loop(reader, writer)
        }
      } yield ()

    def reader(clientSocket: Socket): Resource[F, BufferedReader] =
      Resource.make {
        Sync[F].delay(new BufferedReader(new InputStreamReader(clientSocket.getInputStream)))
      }(reader => Sync[F].delay(reader.close()).handleErrorWith(_ => Sync[F].unit))

    def writer(clientSocket: Socket): Resource[F, BufferedWriter] =
      Resource.make {
        Sync[F].delay(new BufferedWriter(new PrintWriter(clientSocket.getOutputStream)))
      }(writer => Sync[F].delay(writer.close()).handleErrorWith(_ => Sync[F].unit))

    def readerWriter(clientSocket: Socket): Resource[F, (BufferedReader, BufferedWriter)] =
      for {
        reader <- reader(clientSocket)
        writer <- writer(clientSocket)
      } yield (reader, writer)

    readerWriter(clientSocket).use {
      case (reader, writer) =>
        loop(reader, writer) // Let's get to work
    }
  }

  def close[F[_]: Sync](socket: ServerSocket): F[Unit] =
    Sync[F].delay(socket.close()).handleErrorWith(_ => Sync[F].unit)

  def close[F[_]: Sync](socket: Socket): F[Unit] =
    Sync[F].delay(socket.close()).handleErrorWith(_ => Sync[F].unit)

  def serveUncancelable[F[_]: Async](
      serverSocket: ServerSocket,
      shutdown: Deferred[F, Unit]
  ): F[Unit] = {
    val handler: F[Unit] = MonadCancel[F] uncancelable { poll =>
      poll(Async[F].delay(serverSocket.accept())) flatMap { conn =>
        echoProtocol[F](conn).guarantee(close[F](conn)).start.void
      }
    }

    handler.foreverM
  }

  def serveInterruptible[F[_]: Async](
      serverSocket: ServerSocket
  ): F[Unit] =
    for {
      mayBeConn <- Async[F].delay(Try(serverSocket.accept()).toOption)
      r <- mayBeConn match {
        case Some(conn) =>
          println("new connection")
          for {
            _ <- echoProtocol[F](conn).guarantee(close[F](conn)).start.void
            r <- serveInterruptible(serverSocket)
          } yield r
        case None => Async[F].unit
      }
    } yield r

  def run: IO[Unit] =
    for {
      serverSocket <- IO(new ServerSocket(5432))
      _ = sys.addShutdownHook {
        implicit val env = MyIORuntime
        (close[IO](serverSocket) >> IO.println("terminated")).unsafeRunSync()
      }
      _ <- serveInterruptible[IO](serverSocket)

    } yield ()

}

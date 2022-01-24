package example

import cats.effect._
import cats.effect.std.Console
import cats.implicits._

import java.io.{ BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter }
import java.net.{ ServerSocket, Socket }
import scala.util.control.NoStackTrace

object PingPong extends IOApp.Simple {

  def bufferedRW[F[_]: Sync](socket: Socket): Resource[F, (BufferedReader, BufferedWriter)] =
    for {
      reader <- Resource.fromAutoCloseable(
        Sync[F].blocking(new BufferedReader(new InputStreamReader(socket.getInputStream)))
      )
      writer <- Resource.fromAutoCloseable(
        Sync[F].blocking(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream)))
      )
    } yield (reader, writer)

  def resources[F[_]: Sync](
      serverSocket: Socket,
      clientSocket: Socket
  ): Resource[F, (BufferedReader, BufferedWriter, BufferedReader, BufferedWriter)] =
    for {
      (serverReader, serverWriter) <- bufferedRW[F](serverSocket)
      (clientReader, clientWriter) <- bufferedRW[F](clientSocket)
    } yield (serverReader, serverWriter, clientReader, clientWriter)

  def requestReply[F[_]: Async: Console](
      msg: String,
      serverReader: BufferedReader,
      serverWriter: BufferedWriter,
      clientReader: BufferedReader,
      clientWriter: BufferedWriter
  ): F[Boolean] =
    for {
      _ <- Sync[F].blocking {
        clientWriter.write(msg)
        clientWriter.write('\n')
        clientWriter.flush()
      }
      _       <- Console[F].println("client written")
      readMsg <- Sync[F].blocking(serverReader.readLine())
      _       <- Console[F].println("server read")
      _ <- Sync[F].blocking {
        serverWriter.write(readMsg)
        serverWriter.write('\n')
        serverWriter.flush()
      }
      _        <- Console[F].println("server written")
      replyMsg <- Sync[F].blocking(clientReader.readLine())
      _        <- Console[F].println("client read")
    } yield replyMsg == msg

  def handler[F[_]: Async: Console](
      msg: String,
      serverSocket: Socket,
      clientSocket: Socket
  ): F[Boolean] =
    resources[F](serverSocket, clientSocket).use {
      case (sr, sw, cr, cw) =>
        requestReply[F](msg, sr, sw, cr, cw)
    }

  //CE3
  def serverSocket[F[_]: Async]: Resource[F, ServerSocket] =
    Resource.fromAutoCloseable(Sync[F].blocking(new ServerSocket(10559)))

  def clientSocket[F[_]: Async]: Resource[F, Socket] =
    Resource.fromAutoCloseable(Sync[F].blocking(new Socket("localhost", 10559)))

  case object NoAccept extends RuntimeException with NoStackTrace
  val clientAndServer: Resource[IO, (IO[Socket], Socket)] = for {
    serverSocket <- serverSocket[IO]
    serverSocketIO <-
      IO.blocking(serverSocket.accept())
        .background
        .map(_.flatMap {
          case Outcome.Succeeded(fa: IO[Socket]) => fa
          case _                                 => IO.raiseError[Socket](NoAccept)
        })
    clientSocket <- clientSocket[IO]
  } yield (serverSocketIO, clientSocket)

  override def run: IO[Unit] =
    clientAndServer.use {
      case (serverIO, client) =>
        serverIO.flatMap {
          handler[IO]("hello", _, client)
        }
    }.void
}

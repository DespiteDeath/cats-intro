import cats._
import cats.implicits._

import scala.concurrent._
import scala.util.{ Either, _ }

implicit val cte = ExecutionContext.fromExecutor(_.run())

case class Configuration(config: String)

case class Path(path: String)

trait Store[F[_]] {
  def read(path: Path): F[String]
}

object MyOptionStore extends Store[Option] {
  override def read(path: Path): Option[String] = Some("config")
}

object MyOptionStore1 extends Store[Option] {
  override def read(path: Path): Option[String] = None
}

object MyTryStore extends Store[Try] {
  override def read(path: Path): Try[String] = Try("config")
}

val fileNotFound = new RuntimeException("File not found")
object MyTryStore1 extends Store[Try] {
  override def read(path: Path): Try[String] = Failure(fileNotFound)
}

object MyFutureStore extends Store[Future] {
  override def read(path: Path): Future[String] = Future("config")
}

object MyFutureStore1 extends Store[Future] {
  override def read(path: Path): Future[String] = Future.failed(fileNotFound)
}

type MyEither[T] = Either[Throwable, T]

object MyEitherStore extends Store[MyEither] {
  override def read(path: Path): MyEither[String] = Right("config")
}

object MyEitherStore1 extends Store[MyEither] {
  override def read(path: Path): MyEither[String] = Left(fileNotFound)
}

object ConfigurationStorage {

  def fromStore[F[_]: Monad](store: Store[F], path: Path)(implicit
      me: MonadError[F, Throwable]
  ): F[Configuration] =
    for {
      string        <- store.read(path)
      configuration <- parseConfiguration(string).fold(me.raiseError, me.pure)
    } yield configuration

  def parseConfiguration(config: String): Try[Configuration] = Try(Configuration(config))
}

//ConfigurationStorage.fromStore(MyOptionStore, Path("configuration"))
ConfigurationStorage.fromStore(MyTryStore, Path("configuration"))
ConfigurationStorage.fromStore(MyFutureStore, Path("configuration"))
ConfigurationStorage.fromStore(MyEitherStore, Path("configuration"))

ConfigurationStorage.fromStore(MyTryStore1, Path("configuration"))
ConfigurationStorage.fromStore(MyFutureStore1, Path("configuration"))
ConfigurationStorage.fromStore(MyEitherStore1, Path("configuration"))

import scala.concurrent._
import scala.util.{Either, _}
import cats._
import cats.data._
import cats.implicits._
import cats.effect._
import cats.instances.all._

implicit val cte = ExecutionContext.fromExecutor(_.run())

case class Configuration(config: String)

case class Path(path: String)

trait Store[F[_]] {
  def read(path: Path): F[String]
}

object MyOptionStore extends Store[Option] {
  override def read(path: Path) = Some("config")
}

object MyOptionStore1 extends Store[Option] {
  override def read(path: Path) = None
}

object MyTryStore extends Store[Try] {
  override def read(path: Path) = Try("config")
}

object MyTryStore1 extends Store[Try] {
  override def read(path: Path) = Failure(new RuntimeException("File not found"))
}

object MyFutureStore extends Store[Future] {
  override def read(path: Path) = Future("config")
}

object MyFutureStore1 extends Store[Future] {
  override def read(path: Path) = Future.failed(new RuntimeException("File not found"))
}

type MyEither[T] = Either[Throwable, T]

object MyEitherStore extends Store[MyEither] {
  override def read(path: Path) = Right("config")
}

object MyEitherStore1 extends Store[MyEither] {
  override def read(path: Path) = Left(new RuntimeException("File not found"))
}

object ConfigurationStorage {

  def fromStore[F[_] : Monad](store: Store[F], path: Path)
                             (implicit me: MonadError[F, Throwable]): F[Configuration] =
    store.read(path).flatMap { string =>
      parseConfiguration(string) match {
        case Failure(t) => me.raiseError(t)
        case Success(configuration) => me.point(configuration)
      }
    }

  def parseConfiguration(config: String): Try[Configuration] = Try(Configuration(config))
}

//ConfigurationStorage.fromStore(MyOptionStore, Path("configuration"))
ConfigurationStorage.fromStore(MyTryStore, Path("configuration"))
ConfigurationStorage.fromStore(MyFutureStore, Path("configuration"))
ConfigurationStorage.fromStore(MyEitherStore, Path("configuration"))

ConfigurationStorage.fromStore(MyTryStore1, Path("configuration"))
ConfigurationStorage.fromStore(MyFutureStore1, Path("configuration"))
ConfigurationStorage.fromStore(MyEitherStore1, Path("configuration"))

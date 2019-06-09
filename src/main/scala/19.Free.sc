import scala.concurrent._, scala.util._
import cats._, cats.data._, cats.implicits._, cats.effect._, cats.free._

sealed abstract class KVStoreA[A] extends Product with Serializable

final case class Put[T](key: String, value: T) extends KVStoreA[Unit]

final case class Get[T](key: String) extends KVStoreA[Option[T]]

final case class Delete[T](key: String) extends KVStoreA[Option[T]]

type KVStore[A] = Free[KVStoreA, A]

import Free._

def put[T](key: String, value: T): KVStore[Unit] = liftF[KVStoreA, Unit](Put[T](key, value))
def get[T](key: String): KVStore[Option[T]] = liftF[KVStoreA, Option[T]](Get[T](key))
def delete[T](key: String): KVStore[Option[T]] = liftF[KVStoreA, Option[T]](Delete(key))
def update[T](key: String, f: T => T): KVStore[Unit] =
  for {
    vMaybe <- get[T](key)
    _ <- vMaybe.map(v => put[T](key, f(v))).getOrElse(Free.pure(()))
  } yield ()

def program: KVStore[Option[Int]] =
  for {
    _ <- put("wild-cats", 2)
    _ <- update[Int]("wild-cats", _ + 12)
    _ <- put("tame-cats", 5)
    n <- get[Int]("wild-cats")
    _ <- delete[Int]("tame-cats")
  } yield n

val kvs = scala.collection.mutable.Map.empty[String, Any]
def impureCompiler: KVStoreA ~> Id =
  new (KVStoreA ~> Id) {
    // a very simple (and imprecise) key-value store
    def apply[A](fa: KVStoreA[A]): Id[A] =
      fa match {
        case Put(key, value) => kvs(key) = value
        case Get(key) => kvs.get(key)
        case Delete(key) => kvs.remove(key)
      }
  }

program.foldMap(impureCompiler)
println(kvs)

type KVStoreState[A] = State[Map[String, Any], A]
val pureCompiler: KVStoreA ~> KVStoreState = new (KVStoreA ~> KVStoreState) {
  def apply[A](fa: KVStoreA[A]): KVStoreState[A] =
    fa match {
      case Put(key, value) => State.modify(_ + (key -> value))
      case Get(key) => State.inspect(_.get(key))
      case Delete(key) => State.apply(s => (s - key, s.get(key)))

    }
}
program.foldMap(pureCompiler).run(Map.empty).value

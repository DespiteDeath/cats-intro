package example

import cats._
import cats.data._
import cats.effect._
import cats.implicits._

import scala.util.control.NoStackTrace

sealed trait Done
case object Done extends Done

case class Product(id: String, quantity: Int)
case class ShoppingCart(id: String, products: List[Product] = List.empty)
case class ShoppingCartExists(id: String) extends Throwable with NoStackTrace

trait ShoppingCartStore[F[_]] {
  def create(id: String): F[ShoppingCartStore.CreateResult]
  def find(id: String): F[Option[ShoppingCart]]
}

object ShoppingCartStore {

  def apply[F[_]](implicit ev: ShoppingCartStore[F]): ShoppingCartStore[F] = ev
  type CreateResult = ShoppingCartExists Either Done

  type Store = Map[String, ShoppingCart]

  object mapstore {
    type StateBackend[A] = State[Store, A]

    implicit object MapStore extends ShoppingCartStore[StateBackend] {

      override def create(id: String): StateBackend[CreateResult] = {
        def create: StateBackend[Done] =
          State { current: Store =>
            (current + (id -> ShoppingCart(id)), Done)
          }

        def mayBeCreate(mayBeShoppingCart: Option[ShoppingCart]): StateBackend[CreateResult] =
          mayBeShoppingCart match {
            case Some(_) => State.inspect(_ => Either.left(ShoppingCartExists(id)))
            case None    => create.inspect(_ => Either.right(Done))
          }

        for {
          findResult <- find(id)
          result     <- mayBeCreate(findResult)
        } yield result
      }

      override def find(id: String): StateBackend[Option[ShoppingCart]] =
        State.inspect(_.get(id))
    }

  }

  object refstore {

    class Refstore[F[_]: Functor](storeRef: Ref[F, Map[String, ShoppingCart]])
        extends ShoppingCartStore[F] {
      override def create(id: String): F[CreateResult] =
        storeRef.modify { map =>
          map
            .get(id)
            .fold[(Store, CreateResult)] {
              (map + (id -> ShoppingCart(id)), Either.right(Done))
            } { _ =>
              (map, Either.left(ShoppingCartExists(id)))
            }
        }

      override def find(id: String): F[Option[ShoppingCart]] = storeRef.get.map(_.get(id))

    }
    object Refstore {
      def make[F[_]: Ref.Make: Monad]: F[Refstore[F]] =
        for {
          store <- Ref[F].of(Map.empty[String, ShoppingCart])
        } yield new Refstore(store)
    }
  }

}

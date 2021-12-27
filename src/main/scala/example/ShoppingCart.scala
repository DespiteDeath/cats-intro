package example

import cats.data._
import cats.implicits._
import example.ShoppingCartStore.Done

import scala.util.control.NoStackTrace

case object Done extends Done

case class Product(id: String, quantity: Int)
case class ShoppingCart(id: String, products: List[Product] = List.empty)
case class ShoppingCarExists(id: String) extends Throwable with NoStackTrace

trait ShoppingCartStore[F[_]] {
  def create(id: String): F[ShoppingCartStore.CreateResult]
  def find(id: String): F[Option[ShoppingCart]]
}

object ShoppingCartStore {

  def apply[F[_]](implicit ev: ShoppingCartStore[F]): ShoppingCartStore[F] = ev

  trait Done
  type CreateResult = ShoppingCarExists Either Done

  object mapstore {
    type Store           = Map[String, ShoppingCart]
    type StateBackend[A] = State[Store, A]

    implicit object MapStore extends ShoppingCartStore[StateBackend] {

      override def create(id: String): StateBackend[CreateResult] = {
        def create: StateBackend[Done] =
          State { current: Store =>
            (current + (id -> ShoppingCart(id)), Done)
          }

        def mayBeCreate(mayBeShoppingCart: Option[ShoppingCart]): StateBackend[CreateResult] =
          mayBeShoppingCart match {
            case Some(_) => State.inspect(_ => Either.left(ShoppingCarExists(id)))
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

}

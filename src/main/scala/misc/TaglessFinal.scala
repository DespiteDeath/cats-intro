package misc

import cats._
import cats.data._
import cats.implicits._
import misc.ShoppingCarts.ShoppingCartExists

import scala.util.control.NoStackTrace

sealed trait Done
case object Done extends Done

case class Product(id: String, description: String)
case class ShoppingCart(id: String, products: List[Product])

trait ShoppingCarts[F[_]] {
  def create(id: String): F[ShoppingCartExists Either Done]

  def find(id: String): F[Option[ShoppingCart]]

  def add(sc: ShoppingCart, product: Product): F[ShoppingCart]
}

object ShoppingCarts {

  def apply[F[_]](implicit sc: ShoppingCarts[F]): ShoppingCarts[F] = sc

  case class ShoppingCartExists(cartId: String) extends Throwable with NoStackTrace

  def createAndToCart[F[_]: Monad: ShoppingCarts](
      product: Product,
      cartId: String
  ): F[Option[ShoppingCart]] =
    for {
      _          <- ShoppingCarts[F].create(cartId)
      maybeSc    <- ShoppingCarts[F].find(cartId)
      maybeNewSc <- maybeSc.traverse(sc => ShoppingCarts[F].add(sc, product))
    } yield maybeNewSc

}

object ShoppingCartFuture {

  import scala.concurrent.Future

//  implicit object ShoppingCartInterpreter extends ShoppingCarts[Future] {
//    override def create(id: String): Future[Done] = Future.successful(Done)
//
//    override def find(id: String): Future[Option[ShoppingCart]] = ???
//
//    override def add(sc: ShoppingCart, product: Product): Future[ShoppingCart] = ???
//  }
}

object ShoppingCartsState {
  type ShoppingCartRepository = Map[String, ShoppingCart]
  type ScRepoState[A]         = State[ShoppingCartRepository, A]

  implicit object ShoppingCartInterpreter extends ShoppingCarts[ScRepoState] {

    override def create(id: String): ScRepoState[ShoppingCartExists Either Done] =
      State { carts =>
        carts.get(id) match {
          case Some(_) =>
            (carts, Left(ShoppingCartExists(id)))
          case _ =>
            val shoppingCart = ShoppingCart(id, List())
            (carts + (id -> shoppingCart), Right(Done))
        }
      }

    override def find(id: String): ScRepoState[Option[ShoppingCart]] =
      State.inspect { carts =>
        carts.get(id)
      }

    override def add(sc: ShoppingCart, product: Product): ScRepoState[ShoppingCart] =
      State { carts =>
        val products    = sc.products
        val updatedCart = sc.copy(products = product :: products)
        (carts + (sc.id -> updatedCart), updatedCart)
      }
  }

}

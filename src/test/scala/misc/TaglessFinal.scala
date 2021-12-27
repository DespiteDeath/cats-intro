package misc

import misc.ShoppingCarts.ShoppingCartExists
import org.scalacheck.Gen
import org.scalatest.{ EitherValues, OptionValues }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class TaglessFinal
    extends AnyFlatSpec
    with Matchers
    with ScalaCheckDrivenPropertyChecks
    with OptionValues
    with EitherValues {

  behavior of "shopping chart"

  val productGenerator: Gen[Product] = for {
    id   <- Gen.alphaStr
    desc <- Gen.alphaStr
  } yield Product(id, desc)

  it should "be able to create shopping cart" in {
    import ShoppingCartsState._
    forAll(productGenerator, Gen.alphaStr) { (product, cartId) =>
      whenever(product.id.nonEmpty && cartId.nonEmpty) {
        val (repo, maybeShoppingCart) =
          ShoppingCarts.createAndToCart[ScRepoState](product, cartId).run(Map.empty).value
        assertResult(cartId)(maybeShoppingCart.value.id)
        println(repo)
      }
    }
  }

  it should "not be able to create shopping cart again" in {
    import ShoppingCartsState._
    forAll(productGenerator, Gen.alphaStr) { (product, cartId) =>
      whenever(product.id.nonEmpty && cartId.nonEmpty) {
        val (cart, resultDone) = ShoppingCarts[ScRepoState].create(cartId).run(Map.empty).value
        assertResult(Done)(resultDone.value)
        val (_, resultExists) = ShoppingCarts[ScRepoState].create(cartId).run(cart).value
        assertResult(ShoppingCartExists(cartId))(resultExists.left.value)
      }
    }
  }

}

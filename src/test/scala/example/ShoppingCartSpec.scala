package example

import cats.tests.CatsSuite
import org.scalacheck.Gen
import org.scalatest.{ EitherValues, OptionValues }

class ShoppingCartSpec extends CatsSuite with OptionValues with EitherValues {

  test("create a shopping cart") {
    import example.ShoppingCartStore.mapstore._
    forAll(Gen.alphaStr) { shoppingCartId =>
      val (store, created) = ShoppingCartStore[ShoppingCartStore.mapstore.StateBackend]
        .create(shoppingCartId)
        .run(Map.empty)
        .value

      store.keys should contain(shoppingCartId)
      created.value should be(Done)

      val (_, created2) = ShoppingCartStore[ShoppingCartStore.mapstore.StateBackend]
        .create(shoppingCartId)
        .run(store)
        .value

      store.keys should contain(shoppingCartId)
      created2.left.value should be(ShoppingCarExists(shoppingCartId))
    }
  }

  test("get a shopping cart") {
    import example.ShoppingCartStore.mapstore._
    forAll(Gen.alphaStr) { shoppingCartId =>
      val (store, created) = ShoppingCartStore[ShoppingCartStore.mapstore.StateBackend]
        .create(shoppingCartId)
        .run(Map.empty)
        .value

      store.keys should contain(shoppingCartId)
      created.value should be(Done)

      val (_, findResult) = ShoppingCartStore[ShoppingCartStore.mapstore.StateBackend]
        .find(shoppingCartId)
        .run(store)
        .value

      findResult.value shouldBe ShoppingCart(shoppingCartId)
    }
  }

}

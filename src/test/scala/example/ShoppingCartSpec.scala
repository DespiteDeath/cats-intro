package example

import cats.effect._
import cats.implicits._
import org.scalacheck.Gen
import weaver._
import weaver.scalacheck.Checkers

object ShoppingCartSpec extends SimpleIOSuite with Checkers {

  test("create a shopping cart") {
    import example.ShoppingCartStore.mapstore._
    forall(Gen.alphaStr) { shoppingCartId =>
      val (store, created) = ShoppingCartStore[ShoppingCartStore.mapstore.StateBackend]
        .create(shoppingCartId)
        .run(Map.empty)
        .value

      val part1 = expect(store.contains(shoppingCartId)) and expect(created == Right(Done))

      val (_, created2) = ShoppingCartStore[ShoppingCartStore.mapstore.StateBackend]
        .create(shoppingCartId)
        .run(store)
        .value

      part1 and
      expect(store.contains(shoppingCartId)) and
      expect(created2 == Left(ShoppingCartExists(shoppingCartId)))
    }
  }

  test("get a shopping cart") {
    import example.ShoppingCartStore.mapstore._
    forall(Gen.alphaStr) { shoppingCartId =>
      val (store, created) = ShoppingCartStore[ShoppingCartStore.mapstore.StateBackend]
        .create(shoppingCartId)
        .run(Map.empty)
        .value

      val part1 = expect(store.contains(shoppingCartId)) and expect(created == Right(Done))

      val (_, findResult) = ShoppingCartStore[ShoppingCartStore.mapstore.StateBackend]
        .find(shoppingCartId)
        .run(store)
        .value

      part1 and
      expect(findResult.contains(ShoppingCart(shoppingCartId)))

    }
  }

  test("create a shopping cart") {
    forall(Gen.alphaStr) { shoppingCartId =>
      example.ShoppingCartStore.refstore.Refstore.make[IO].flatMap { implicit store =>
        for {
          create1 <- ShoppingCartStore[IO].create(shoppingCartId)
          find1   <- ShoppingCartStore[IO].find(shoppingCartId)
          a1 = assert(find1.isDefined) and assert(create1 == Right(Done))
          result2 <- ShoppingCartStore[IO].create(shoppingCartId)
          a2 = assert(result2 == Left(ShoppingCartExists(shoppingCartId)))
        } yield a1 and a2
      }
    }
  }

}

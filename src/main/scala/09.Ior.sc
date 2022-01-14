
import cats.data._
import cats.implicits._

Ior.right[String, Int](3)
Ior.left[String, Int]("Error")

case class Person(name: String, age: Int)

(
  Ior.both(NonEmptyChain.one("Warning 1"), "name"),
  Ior.both(NonEmptyChain.one("Warning 2"), 21)
)
  .mapN(Person.apply)

(
  Ior.left(NonEmptyChain.one("hard failure")),
  Ior.both(NonEmptyChain.one("Warning 2"), 21)
)
  .mapN(Person.apply)

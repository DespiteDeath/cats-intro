package misc

import org.mockito.MockitoSugar
import org.scalacheck.Gen
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent._

class KeepEmptySpec
    extends AnyFlatSpec
    with ScalaCheckPropertyChecks
    with MockitoSugar
    with Matchers
    with EitherValues {

  behavior of "nonsense"

  case class Person(name: String, age: Int)

  val personGen: Gen[Person] = for {
    name <- Gen.alphaStr
    age  <- Gen.posNum[Int]
  } yield Person(name, age)

  val persons: Gen[List[Person]] = for {
    size    <- Gen.choose(5, 10)
    persons <- Gen.listOfN(size, personGen)
  } yield persons

  it should "sort people" in {
    forAll(persons) { persons =>
      //implicit val m1: Ordering[Person] = Ordering.by[Person, String](_.name).orElseBy(_.age)
      implicit val m2: Ordering[Person] = Ordering[(Int, String)].on[Person](p => (p.age, p.name))
      println(persons.sorted.mkString(System.lineSeparator()))
      println("----------")
    }
  }

  it should "do something" in {
    import cats._
    import cats.data._
    import cats.implicits._
    import scala.util.chaining._
    implicit val exc = ExecutionContext.fromExecutor(_.run())

  }

}

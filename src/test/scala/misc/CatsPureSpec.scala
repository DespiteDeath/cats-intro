package misc

import cats._
import cats.arrow.FunctionK
import cats.data.Kleisli
import cats.implicits._
import weaver.SimpleIOSuite

import scala.util._

object CatsPureSpec extends SimpleIOSuite {

  case class Klass(x: Int)

  object Klass {

    implicit val monoid: Monoid[Klass] =
      Monoid.instance[Klass](Klass(0), (k1, k2) => Klass(k1.x + k2.x))

    implicit val semigroup: Semigroup[Klass] =
      Semigroup.instance[Klass]((k1, k2) => Klass(k1.x + k2.x))

    implicit val show: Show[Klass] = Show.show[Klass](_.toString)

    implicit val eq: Eq[Klass] = Eq.instance[Klass]((k1, k2) => k1.x == k2.x)

    implicit val ordered: Ordering[Klass] = Ordering.by[Klass, Int](_.x)
  }

  pureTest("Monoid") {
    def m[A: Monoid]: A = Monoid[A].empty |+| Monoid[A].empty
    expect(m[Klass] == Monoid[Klass].empty) and
    expect(Iterator.range(1, 10).map(Klass.apply).toList.reduce(_ |+| _) == Klass(45))
  }

  pureTest("MonoidK-SemigroupK") {
    expect(MonoidK[List].empty[String] == List.empty[String]) and
    expect(MonoidK[Option].empty[String] == Option.empty[String]) and
    expect(MonoidK[Option].combineK(Option(1), Option(2)) == Option(1)) and
    expect(MonoidK[List].combineK(List(1), List(2)) == List(1, 2))
  }

  pureTest("Alternative") {
    def even(x: Int): Int Either Int = Either.cond(x % 2 == 0, x, x)
    expect((List(even _) <*> Iterator.range(1, 5).toList).separate == (List(1, 3), List(2, 4)))
  }

  pureTest("sort") {
    println(scala.util.Random.nextBytes(10).map(it => Klass(it.toInt)).sorted.mkString)
    assert(true)
  }

  pureTest("show") {
    expect(Klass(0).show == Klass(0).toString)
  }

  pureTest("eq") {
    expect(Klass(1) === Klass(1)) and
    expect(Klass(1) =!= Klass(2))
  }

  pureTest("MonoidK-FunctionK") {
    val firstV1: FunctionK[List, Option] = λ[FunctionK[List, Option]](_.headOption)
    val firstV2: List ~> Option          = λ[List ~> Option](_.headOption)
    val firstV3: Option ~> List          = λ[Option ~> List](_.toList)

    assert(MonoidK[List].empty[String] == List.empty[String]) and
    assert(MonoidK[Option].empty[String].isEmpty) and
    assert(firstV1(List.empty[String]).isEmpty) and
    assert(firstV2(List.empty[String]).isEmpty) and
    assert(firstV3(Option.empty[String]).isEmpty)
  }

  def parseInt(s: String): Option[Int] = scala.util.Try(Integer.parseInt(s)).toOption

  pureTest("traverse") {
    assert(Foldable[List].traverse_(List("1", "2"))(parseInt).contains(())) and
    assert(Traverse[List].traverse(List("1", "2"))(parseInt).contains(List(1, 2)))
  }

  pureTest("catchOnly") {
    assert(Either.catchOnly[NumberFormatException]("a".toInt).isLeft)
  }

  pureTest("Kleisli") {
    val parse: String => Option[Int] =
      s => if (s.matches("-?[0-9]+")) Some(s.toInt) else None

    val reciprocal: Int => Option[Double] =
      i => if (i != 0) Some(1.0 / i) else None

    val r = for {
      k <- parse("2")
      t <- reciprocal(k)
    } yield t

    assert(r.contains(.5)) and
    assert(Kleisli(reciprocal).compose(parse).run("2").contains(.5)) and
    assert(Kleisli(parse).andThen(reciprocal).run("2").contains(.5))
  }

  pureTest("SAM") {
    def twice[A: Semigroup](a: A): A = a |+| a
    val fst: Int                     = twice(3)
    val snd: Int                     = twice(3)(_ * _)
    assert(fst == 6) and assert(snd == 9)
  }

  pureTest("contravariant box") {
    trait Box[-T] {
      def m(t: T): Unit
    }
    implicit object ContravariantForBox extends Contravariant[Box] {
      override def contramap[A, B](fa: Box[A])(f: B => A): Box[B] = (b: B) => fa.m(f(b))
    }
    object BoxInt extends Box[Int] {
      override def m(t: Int): Unit = println(t)
    }
    val boxBoolean: Box[Boolean] =
      Contravariant[Box].contramap(BoxInt)((b: Boolean) => if (b) 1 else 0)
    boxBoolean.m(false)
    Passed
  }

}

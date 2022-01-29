
import cats.Semigroup
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

def combineT[E: Semigroup, A, B](e1: Either[E, A], e2: Either[E, B]): Either[E, (A, B)] =
  (e1, e2) match {
    case (Left(l1), Left(l2))   => Left(l1 |+| l2)
    case (Left(l1), _)          => Left(l1)
    case (_, Left(l2))          => Left(l2)
    case (Right(r1), Right(r2)) => Right((r1, r2))
  }

def combineAll[E: Semigroup, A](es: List[Either[E, A]]): Either[E, List[A]] =
  es.foldRight[Either[E, List[A]]](Either.right(List.empty)) {
    case (Left(l1), Left(l2))   => Left(l1 |+| l2)
    case (Left(l1), Right(_))   => Left(l1)
    case (Right(_), Left(l2))   => Left(l2)
    case (Right(r1), Right(r2)) => Right(r2 :+ r1)
  }

combineAll(List(Left(1), Left(2)))
combineAll(List[Either[Int, Int]](Right(1), Right(2)))

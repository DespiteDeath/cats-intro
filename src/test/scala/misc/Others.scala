package misc

import weaver._

object Others extends FunSuite {

  test("custom take and group") {
    def take[A](l: List[A], n: Int): (List[A], List[A]) = {
      def take1[A](t: List[A], rest: List[A], n: Int): (List[A], List[A]) =
        rest match {
          case Nil         => (t, Nil)
          case _ if n == 0 => (t, rest)
          case e :: rest   => take1(t :+ e, rest, n - 1)
        }
      take1(List.empty[A], l, n)
    }

    def group[A](l: List[A], c: Int): List[List[A]] =
      take(l, c) match {
        case (Nil, Nil) => Nil
        case (g, Nil)   => List(g)
        case (g, rest)  => g +: group(rest, c)
      }

    assert(take(Nil, 0) == (Nil, Nil)) &&
    assert(take(Nil, 2) == (Nil, Nil)) &&
    assert(take(List(1, 2), 2) == (List(1, 2), Nil)) &&
    assert(group(List(1, 2), 1) == List(List(1), List(2))) &&
    assert(group(List(1, 2, 3), 1) == List(List(1), List(2), List(3))) &&
    assert(group(List(1, 2, 3), 2) == List(List(1, 2), List(3)))
  }
}

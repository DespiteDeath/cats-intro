import cats._, cats.data._, cats.implicits._

def add[A: Monoid](a1: A, a2: A): A =
    Monoid[A].combine(a1, a2)

add(1, 2)

add("mert", "inan")



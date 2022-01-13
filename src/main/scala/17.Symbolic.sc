import cats.effect._
import cats.implicits._

//https://typelevel.org/cats/faq.html

//usual scala issue of overly refining types of ADTs
//Some(1) >>= (x => Some(x + 1))

1.some >>= (x => Some(x + 1))
Option(1) >>= (x => Some(x + 1))
None >> Option(2)
Option(1) >> Option(2)
Option(1) *> Option(2)
Option((_: Int) + 1) <*> Option(11)
Option((_: Int) + 1).ap(Option(11))
Option((_: Int) + (_: Int)).ap2(Option(1), Option(2))

IO.never *> IO(println("done"))

//implicit val errorOrFirst: List ~> Option = λ[List ~> Option](_.headOption)
//errorOrFirst(List(199))

val map1 = Map("a" -> 1, "b" -> 2)
val map2 = Map("b" -> 3, "d" -> 4)
map1 |+| map2

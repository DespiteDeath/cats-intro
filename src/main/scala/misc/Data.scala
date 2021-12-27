package misc

import cats.data._

object Data {

  val reader: Reader[Int, (Int, Int)] = {
    for {
      k <- Reader[Int, Int](_ + 1)
      t <- Reader[Int, Int](_ + 2)
    } yield (k, t)
  }

  println(reader.run(2))

  val myState: State[Int, (Int, String, Int, Int)] = {
    for {
      a      <- State.inspect((_: Int) + 1)
      string <- State.inspect("hello ".repeat(_: Int).stripTrailing())
      s      <- State.get
      _      <- State.modify((_: Int) * 2)
      t      <- State.get
    } yield (a, string, s, t)
  }

  println(myState.run(2).value)

  def main(args: Array[String]): Unit = ()

}

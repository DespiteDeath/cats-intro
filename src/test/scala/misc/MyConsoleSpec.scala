package misc

import cats.effect._
import cats.effect.testing.scalatest._
import cats.implicits._
import misc.MyConsole.ref.ConsoleRef
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class MyConsoleSpec extends AsyncFlatSpec with AsyncIOSpec with Matchers {

  behavior of "my program"

  it should "read user name" in {
    import MyConsole.state._
    val (list, _) = MyConsole.readUserName[ConsoleState]().runEmpty.value
    list shouldBe List("Enter your name", "Your name's test")
  }

  it should "read user name 2 " in {
    for {
      console <- ConsoleRef.make[IO]
      implicit0(c: MyConsole[IO]) = console
      _     <- MyConsole.readUserName[IO]()
      state <- console.ref.get
    } yield state shouldBe List("Enter your name", "Your name's test")
  }

}

package misc

import cats.effect._
import cats.effect.testing.scalatest._
import cats.implicits._
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
      ref <- Ref.of[IO, List[String]](List.empty)
      implicit0(c: MyConsole[IO]) = new MyConsole.ref.ConsoleRef[IO](ref)
      _     <- MyConsole.readUserName[IO]()
      state <- ref.get
    } yield state shouldBe List("Enter your name", "Your name's test")
  }

}

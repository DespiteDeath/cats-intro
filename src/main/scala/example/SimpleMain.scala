package example

import cats.effect._

object SimpleMain {

  def main(args: Array[String]): Unit = {
    implicit val t = MyIORuntime

    val joe = for {
      waiter <- IO.deferred[Int]
      _ = sys.addShutdownHook {
        println("terminating1")
        waiter.complete(1).unsafeRunSync()
        println("terminating2")
      }
      _ <- IO.println("mert")
      t <- waiter.get
      _ <- IO.println(t)
    } yield ()
    joe.unsafeRunSync()
    println("here222")
    System.exit(0)
  }

}

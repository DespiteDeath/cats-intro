package misc

import cats.effect._
import cats.implicits._
import weaver._

import scala.concurrent.duration._

object MySemaphoreSpec extends SimpleIOSuite {

  trait Semaphore {
    def acquire: IO[Unit]
    def release: IO[Unit]
  }

  object Semaphore {
    type Permits = List[Deferred[IO, Unit]]
    def apply(permits: Int): IO[Semaphore] =
      for {
        refI <- Ref.of[IO, Permits](List.empty)
      } yield new Semaphore {
        override def acquire: IO[Unit] = ???
        override def release: IO[Unit] = ???
      }
  }

  test("Warming up with ref") {
    val m = for {
      refI    <- Ref[IO].of(0)
      updateF <- (refI.update(_ + 1) >> IO.sleep(1.second)).foreverM.start
      _       <- IO.sleep(5.seconds) >> updateF.cancel
      i       <- refI.get
    } yield i

    m.map(i => assert(i == 5))
  }

  test("Warming up with Defer") {
    val m = for {
      promise <- IO.deferred[Int]
      f       <- (IO.sleep(2.seconds) >> promise.complete(2) >> promise.complete(1)).start
      i       <- promise.get.flatTap(IO.println)
      _       <- f.join
    } yield i

    m.map(i => assert(i == 2))
  }

  test("Implement Semaphore in terms of Ref and Deferred".only) {
    val m = for {
      semaphore <- Semaphore(1)
      _         <- semaphore.acquire
      _         <- (IO.println("will wait") >> semaphore.acquire >> IO.println("done waiting")).start
      _         <- semaphore.release
    } yield ()

    m.map(_ => assert(true))
  }
}

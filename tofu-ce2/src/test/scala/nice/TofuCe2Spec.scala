package nice

import cats.Applicative
import cats.effect.implicits._
import cats.effect.{ Concurrent, IO }
import cats.implicits._
import tofu.common.Console
import tofu.concurrent._
import weaver.SimpleIOSuite

object TofuCe2Spec extends SimpleIOSuite {

  test("ce2") {

    def example1[F[_]: Agents: Console: Concurrent]: F[Int] =
      for {
        agent <- Agent.Make[F].of(0)
        fibers <-
          agent
            .updateM(a => Console[F].putStrLn(s"current value is $a") *> Applicative[F].pure(a + 1))
            .start
            .replicateA(10)
        _        <- fibers.traverse_(_.join)
        newValue <- agent.get
        _        <- Console[F].putStrLn(s"new value is $newValue")
      } yield newValue

    example1[IO].map(v => assert(v == 10))
  }
}

package misc

import cats._
import cats.effect._
import cats.implicits._
import fs2._
import fs2.concurrent._
import weaver._

import scala.concurrent.duration._

object FS2Spec extends SimpleIOSuite {

  test("process in chunks") {
    def writeToSocket[F[_]: Async](chunk: Chunk[String]): F[Unit] =
      Async[F].async { callback =>
        println(s"[thread: ${Thread.currentThread().getName}] :: Writing $chunk to socket")
        callback(().asRight[Throwable])
        Applicative[F].pure(None) //cleanup
      }

    Stream((1 to 100).map(_.toString): _*)
      .chunkN(10)
      .covary[IO]
      .parEvalMapUnordered(10)(writeToSocket[IO])
      .compile
      .drain
  }

  test("process in chunks2") {
    val s = Stream(1, 2) ++ Stream(3) ++ Stream(4, 5, 6)
    s.covary[IO].chunks.compile.toList >>= IO.println
  }

  test("parallel drain") {
    val s: Stream[Pure, Long] = Stream.range(1L, 5L)
    val compute: Stream[IO, Unit] =
      s.parEvalMap[IO, Unit](10)(i => IO.sleep(i.seconds) *> printCurrentThreadIO())

    compute.compile.drain.timed.flatMap(IO.println)
  }

  test("resource") {
    Stream
      .range(1, 10)
      .covary[IO]
      .compile
      .resource
      .toList
      .use(IO.println)
  }

  test("scan1") {
    val m = Stream.constant[IO, Int](1).scan1(_ + _)
    m.take(10).compile.toList >>= IO.println
  }

  test("concurrently1") {
    val m = for {
      m1 <- Stream.eval(SignallingRef[IO, Int](0))
      _ <- Stream(m1).concurrently(
        Stream.awakeEvery(1.second).zipRight(Stream.range(1, 10).evalMap(m1.set))
      )
      m3 <- m1.discrete
    } yield m3

    m.evalTap(IO.println).take(10).compile.lastOrError.map(it => assert(it == 9))
  }

  test("concurrently2") {
    val data: Stream[IO, Int] = Stream.range(1, 10).covary[IO]
    Stream
      .eval(SignallingRef[IO, Int](0))
      .flatMap(s => Stream(s).concurrently(data.evalMap(s.set)))
      .flatMap(_.discrete)
      .takeWhile(_ < 9, takeFailure = true)
      .compile
      .last
      .map(it => assert(it.contains(9)))
  }

  test("awakeEvery") {
    Stream
      .awakeEvery[IO](1.second)
      .zipRight(Stream.iterate(1)(_ + 1))
      .evalMap(IO.println)
      .compile
      .drain
      .timeout(5.seconds) !> IO.unit
  }

  test("retry") {
    Stream
      .retry[IO, Int](
        fo = IO.println("retrying") >> IO.raiseError[Int](new RuntimeException),
        delay = 1.second,
        nextDelay = _ * 2L,
        maxAttempts = 4
      )
      .compile
      .drain
      .void
  }

  val s1: Stream[IO, Unit] = Stream
    .awakeEvery[IO](500.millis)
    .zipRight(Stream.repeatEval(IO.println("s1")))
  val s2: Stream[IO, Unit] = Stream
    .awakeEvery[IO](1000.millis)
    .zipRight(Stream.repeatEval(IO.println("s2")))
  val s3: Stream[IO, Unit] = Stream
    .awakeEvery[IO](1500.millis)
    .zipRight(Stream.repeatEval(IO.println("s3")))

  test("parJoinUnbounded") {
    Stream(s1, s2, s3).covary[IO].parJoinUnbounded.timeout(5.seconds).compile.drain !> IO.unit
  }
  test("parJoin maxOpen=1") {
    Stream(s1, s2, s3).covary[IO].parJoin(1).timeout(5.seconds).compile.drain !> IO.unit
  }
  test("parJoin maxOpen=2") {
    Stream(s1, s2, s3).covary[IO].parJoin(2).timeout(5.seconds).compile.drain !> IO.unit
  }
  test("parJoin maxOpen=3") {
    Stream(s1, s2, s3).covary[IO].parJoin(3).timeout(5.seconds).compile.drain !> IO.unit
  }
  test("merge") {
    s1.merge(s2).timeout(5.seconds).compile.drain !> IO.unit
  }

  test("topic") {
    Topic[IO, String]
      .flatMap { topic =>
        val publisher  = Stream.constant("1").covary[IO].through(topic.publish)
        val subscriber = topic.subscribe(maxQueued = 10).take(4).evalTap(IO.println)
        subscriber.concurrently(publisher).compile.toVector
      } >>= IO.println
  }

  test("SignallingRef".only) {
    SignallingRef[IO, Boolean](false).flatMap { signal =>
      val s1 = Stream.awakeEvery[IO](1.second).interruptWhen(signal)
      val s2 = Stream.sleep[IO](4.seconds) >> Stream.eval(signal.set(true))
      s1.concurrently(s2).compile.toVector
    } >>= IO.println
  }

  test("pull") {
    val p1: Pull[Pure, Int, Unit] = Pull.output1(1)
    val s1                        = p1.stream
    val p2: Pull[Pure, Int, Unit] = p1 >> Pull.output1(2)
    s1.pull.echo
    p2.stream.compile.toList >>= IO.println
  }

  val err = Stream.raiseError[IO](new Exception("oh noes!"))

  test("resource") {
    val count   = new java.util.concurrent.atomic.AtomicLong(0)
    val acquire = IO.println("incremented: " + count.incrementAndGet)
    val release = IO.println("decremented: " + count.decrementAndGet)
    Stream
      .bracket(acquire)(_ => release)
      .flatMap(_ => Stream(1, 2, 3) ++ err)
      .compile
      .drain
  }

  test("mics1") {
    (
      Stream(1, 2, 3).covary[IO] ++
      err ++
      Stream.eval(IO(4))
    ).compile.toList >>= IO.println
  }

  test("mics2") {
    val m = Pull.output(Chunk(1, 2, 3, 4, 5, 6))
    m.stream.evalMap(IO.println).compile.drain
  }

  test("mics3") {

    IO.unit
  }

  test("mics4") {

    IO.unit
  }

  test("mics5") {

    IO.unit
  }

  test("mics6") {

    IO.unit
  }

  test("mics7") {

    IO.unit
  }

}

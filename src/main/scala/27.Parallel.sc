import cats.data._
import cats.effect.{ExitCode, IO}
import cats.implicits._
import example.putStrLine

NonEmptyList
  .of(1, 2, 3)
  .traverse(i => Option(i))

NonEmptyList
  .of(1, 2, 3)
  .parTraverse(i => putStrLine[IO](i.toString))

val error1 = new Exception("exception1")
val error2 = new Exception("exception2")

val m1 = Either.left[NonEmptyList[Throwable], Int](NonEmptyList.one(error1))
val m2 = Either.left[NonEmptyList[Throwable], Int](NonEmptyList.one(error2))
m1 |+| m2

val m11 = Either.right[NonEmptyList[Throwable], Int](1)
val m22 = Either.right[NonEmptyList[Throwable], Int](2)
m11 |+| m22

val v1 = Validated.invalidNel[Throwable, Int](error1)
val v2 = Validated.invalidNel[Throwable, Int](error2)
v1 |+| v2

val v11 = Validated.validNel[Throwable, Int](1)
val v22 = Validated.validNel[Throwable, Int](2)
v11 |+| v22

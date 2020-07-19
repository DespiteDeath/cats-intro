import cats.Eval

object OddEven {
  def odd(n: Int): Eval[String] = even(n - 1)

  def even(n: Int): Eval[String] =
    Eval.now(n <= 0).flatMap {
      case true => Eval.now("done")
      case _ => odd(n - 1)
    }
}

OddEven.even(200001).value

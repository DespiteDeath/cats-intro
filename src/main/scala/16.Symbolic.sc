import scala.concurrent._, scala.util._
import cats._, cats.data._, cats.implicits._

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

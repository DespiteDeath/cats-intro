import cats.implicits._

sealed trait TrafficLight extends Product with Serializable

object TrafficLight {

  case object Red    extends TrafficLight
  case object Yellow extends TrafficLight
  case object Green  extends TrafficLight

  def red: TrafficLight    = Red
  def yellow: TrafficLight = Yellow
  def green: TrafficLight  = Green

  implicit val k: cats.Eq[TrafficLight] = cats.Eq.fromUniversalEquals

}

1 === 1
TrafficLight.Red === TrafficLight.Red


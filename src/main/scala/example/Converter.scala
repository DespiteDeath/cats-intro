package example

import cats.effect._
import fs2.{ Stream, io, text }

import java.nio.file.Paths

object Converter extends IOApp.Simple {

  val converter: Stream[IO, Unit] = {
    def fahrenheitToCelsius(f: Double): Double = (f - 32.0) * (5.0 / 9.0)

    io.file
      .Files[IO]
      .readAll(Paths.get("testdata/fahrenheit.txt"), 4096)
      .through(text.utf8Decode)
      .through(text.lines)
      .filter(s => s.trim.nonEmpty && !s.startsWith("//"))
      .map(line => fahrenheitToCelsius(line.toDouble).toString)
      .intersperse("\n")
      .through(text.utf8Encode)
      .through(
        io.file.Files[IO].writeAll(Paths.get("testdata/celsius.txt"))
      )
  }

  def run: IO[Unit] = converter.compile.drain
}

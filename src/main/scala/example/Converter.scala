package example

import cats.effect._
import fs2.io.file.{ Flags, Path }
import fs2.{ Stream, io, text }

object Converter extends IOApp.Simple {

  val converter: Stream[IO, Unit] = {
    def fahrenheitToCelsius(f: Double): Double = (f - 32.0) * (5.0 / 9.0)

    io.file
      .Files[IO]
      .readAll(Path("testdata/fahrenheit.txt"), 4096, Flags.Read)
      .through(text.utf8.decode)
      .through(text.lines)
      .filter(s => s.trim.nonEmpty && !s.startsWith("//"))
      .map(line => fahrenheitToCelsius(line.toDouble).toString)
      .intersperse("\n")
      .through(text.utf8.encode)
      .through(
        io.file.Files[IO].writeAll(Path("testdata/celsius.txt"), Flags.Write)
      )
  }

  def run: IO[Unit] = converter.compile.drain
}

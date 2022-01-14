import scala.language.postfixOps
import scala.sys.process.Process

val gitHeadCommitSha = taskKey[String]("Determines the current git commit SHA")
val makeVersionProperties =
  taskKey[Seq[File]]("Creates a version.properties file we can find at runtime.")

ThisBuild / gitHeadCommitSha := Process("git rev-parse HEAD").lineStream.head

Global / cancelable := true
testFrameworks += new TestFramework("munit.Framework")
testFrameworks += new TestFramework("weaver.framework.CatsEffect")

lazy val root = (project in file(".")).settings(
  addCompilerPlugin("org.typelevel" % "kind-projector_2.13.6" % "0.13.2"),
  addCompilerPlugin("com.olegpy"   %% "better-monadic-for"    % "0.3.1"),
  inThisBuild(
    Seq(
      version := "0.1",
      scalaVersion := "2.13.6",
      organization := "org.inanme",
      semanticdbEnabled := true,
      semanticdbVersion := "4.4.18"
    )
  ),
  name := "cats-intro",
  scalacOptions ++= Seq(
      //A -X option suggests permanence, while a -Y could disappear at any time
      "-encoding",
      "UTF-8",                         // source files are in UTF-8
      "-explaintypes",                 // Explain type errors in more detail.
      "-deprecation",                  // warn about use of deprecated APIs
      "-unchecked",                    // warn about unchecked type parameters
      "-feature",                      // warn about misused language features
      "-language:postfixOps",          // allow higher kinded types without `import scala.language.postfixOps`
      "-language:higherKinds",         // allow higher kinded types without `import scala.language.higherKinds`
      "-language:implicitConversions", // Allow definition of implicit functions called views
      "-language:existentials",        // Existential types (besides wildcard types) can be written and inferred
      "-language:reflectiveCalls",
      //"-Xlint", // enable handy linter warnings
      //"-Xfatal-warnings", // turn compiler warnings into errors
      "-Yrangepos",
      "-Ymacro-annotations"
    ),
  libraryDependencies ++= Seq(
      "org.typelevel"              %% "cats-core"                     % "2.7.0" withSources,
      "org.typelevel"              %% "cats-free"                     % "2.7.0" withSources,
      "org.typelevel"              %% "cats-testkit-scalatest"        % "2.1.5"    % Test withSources,
      "org.typelevel"              %% "cats-effect"                   % "3.3.1" withSources,
      "org.typelevel"              %% "cats-effect-std"               % "3.3.1" withSources,
      "org.typelevel"              %% "cats-effect-kernel"            % "3.3.4" withSources,
      "org.typelevel"              %% "cats-effect-testkit"           % "3.3.1"    % Test withSources,
      "org.typelevel"              %% "cats-effect-testing-scalatest" % "1.4.0"    % Test withSources,
      "co.fs2"                     %% "fs2-core"                      % "3.2.4" withSources,
      "co.fs2"                     %% "fs2-io"                        % "3.2.4" withSources,
      "co.fs2"                     %% "fs2-scodec"                    % "3.2.4" withSources,
      "org.typelevel"              %% "cats-tagless-macros"           % "0.14.0" withSources,
      "org.typelevel"              %% "simulacrum"                    % "1.0.1" withSources,
      "org.scalatest"              %% "scalatest"                     % "3.2.10"   % Test withSources,
      "org.scalatestplus"          %% "scalacheck-1-15"               % "3.2.10.0" % Test withSources,
      "org.mockito"                %% "mockito-scala-scalatest"       % "1.16.49"  % Test withSources,
      "org.scalacheck"             %% "scalacheck"                    % "1.15.4"   % Test withSources,
      "org.typelevel"              %% "scalacheck-effect"             % "1.0.3"    % Test withSources,
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.15"     % "1.3.0"    % Test withSources,
      "org.scalameta"              %% "munit"                         % "0.7.28"   % Test withSources,
      "org.typelevel"              %% "munit-cats-effect-3"           % "1.0.7"    % Test withSources,
      "org.http4s"                 %% "http4s-dsl"                    % "0.23.7" withSources,
      "org.http4s"                 %% "http4s-blaze-server"           % "0.23.7" withSources,
      "org.http4s"                 %% "http4s-blaze-client"           % "0.23.7" withSources,
      "org.http4s"                 %% "http4s-circe"                  % "0.23.7" withSources,
      "com.chuusai"                %% "shapeless"                     % "2.3.7" withSources,
      "io.circe"                   %% "circe-core"                    % "0.14.1" withSources,
      "io.circe"                   %% "circe-generic"                 % "0.14.1" withSources,
      "io.circe"                   %% "circe-parser"                  % "0.14.1" withSources,
      "com.ringcentral"            %% "cassandra4io"                  % "0.1.10" withSources,
      "com.disneystreaming"        %% "weaver-cats"                   % "0.7.9"    % Test withSources,
      "com.disneystreaming"        %% "weaver-scalacheck"             % "0.7.9"    % Test withSources,
      "tf.tofu"                    %% "tofu-core-ce3"                 % "0.10.6" withSources
    ),
  makeVersionProperties := {
    val propFile = (Compile / resourceManaged).value / "version.properties"
    val content  = "version=%s" format gitHeadCommitSha.value
    IO.write(propFile, content)
    Seq(propFile)
  },
  Compile / resourceGenerators += makeVersionProperties
)

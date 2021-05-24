import scala.sys.process.Process

val gitHeadCommitSha = taskKey[String]("Determines the current git commit SHA")
val makeVersionProperties =
  taskKey[Seq[File]]("Creates a version.properties file we can find at runtime.")

ThisBuild / gitHeadCommitSha := Process("git rev-parse HEAD").lineStream.head

Global / cancelable := true
testFrameworks += new TestFramework("munit.Framework")

lazy val root = (project in file(".")).settings(
  addCompilerPlugin("org.typelevel" % "kind-projector_2.13.6" % "0.13.0"),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
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
      "org.typelevel"              %% "cats-core"                 % "2.6.1" withSources,
      "org.typelevel"              %% "cats-free"                 % "2.6.1" withSources,
      "org.typelevel"              %% "cats-effect"               % "3.1.1" withSources,
      "org.typelevel"              %% "cats-effect-std"           % "3.1.1" withSources,
      "org.typelevel"              %% "cats-effect-kernel"        % "3.1.1" withSources,
      "org.typelevel"              %% "cats-effect-testkit"       % "3.1.1"   % Test withSources,
      "co.fs2"                     %% "fs2-core"                  % "3.0.3" withSources,
      "co.fs2"                     %% "fs2-io"                    % "3.0.3" withSources,
      "org.typelevel"              %% "cats-tagless-macros"       % "0.14.0" withSources,
      "org.typelevel"              %% "simulacrum"                % "1.0.1" withSources,
      "org.scalatest"              %% "scalatest"                 % "3.2.9"   % Test withSources,
      "org.scalatestplus"          %% "scalacheck-1-14"           % "3.2.2.0" % Test withSources,
      "org.mockito"                 % "mockito-core"              % "3.10.0"  % Test withSources,
      "org.mockito"                %% "mockito-scala"             % "1.16.37" % Test withSources,
      "org.scalacheck"             %% "scalacheck"                % "1.14.3"  % Test withSources,
      "org.typelevel"              %% "scalacheck-effect"         % "1.0.2"   % Test withSources,
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.5"   % Test withSources,
      "org.scalameta"              %% "munit"                     % "0.7.26"  % Test withSources,
      "org.typelevel"              %% "munit-cats-effect-3"       % "1.0.3"   % Test withSources
    ),
  makeVersionProperties := {
    val propFile = (Compile / resourceManaged).value / "version.properties"
    val content  = "version=%s" format gitHeadCommitSha.value
    IO.write(propFile, content)
    Seq(propFile)
  },
  Compile / resourceGenerators += makeVersionProperties
)

import scala.sys.process.Process

val gitHeadCommitSha = taskKey[String]("Determines the current git commit SHA")
val makeVersionProperties = taskKey[Seq[File]]("Creates a version.properties file we can find at runtime.")

gitHeadCommitSha in ThisBuild := Process("git rev-parse HEAD").lineStream.head

cancelable in Global := true

lazy val root = (project in file(".")).settings(
  addCompilerPlugin("org.typelevel" % "kind-projector_2.13.2" % "0.11.0"),
  inThisBuild(Seq(
    version := "0.1",
    scalaVersion := "2.13.2",
    organization := "org.inanme",
  )),
  name := "cats-intro",
  scalacOptions ++= Seq(
    //A -X option suggests permanence, while a -Y could disappear at any time
    "-encoding", "UTF-8", // source files are in UTF-8
    "-explaintypes", // Explain type errors in more detail.
    "-deprecation", // warn about use of deprecated APIs
    "-unchecked", // warn about unchecked type parameters
    "-feature", // warn about misused language features
    "-language:postfixOps", // allow higher kinded types without `import scala.language.postfixOps`
    "-language:higherKinds", // allow higher kinded types without `import scala.language.higherKinds`
    "-language:implicitConversions", // Allow definition of implicit functions called views
    "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
    "-language:reflectiveCalls",
    //"-Xlint", // enable handy linter warnings
    //"-Xfatal-warnings", // turn compiler warnings into errors
    "-Yrangepos",
  ),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % "2.1.1" withSources(),
    "org.typelevel" %% "cats-free" % "2.1.1" withSources(),
    "org.typelevel" %% "cats-effect" % "2.1.3" withSources(),
    "co.fs2" %% "fs2-core" % "2.3.0" withSources(),
    "co.fs2" %% "fs2-io" % "2.3.0" withSources(),
    "org.typelevel" %% "cats-tagless-macros" % "0.11" withSources()
  ),
  makeVersionProperties := {
    val propFile = (resourceManaged in Compile).value / "version.properties"
    val content = "version=%s" format gitHeadCommitSha.value
    IO.write(propFile, content)
    Seq(propFile)
  },
  resourceGenerators in Compile += makeVersionProperties,
)

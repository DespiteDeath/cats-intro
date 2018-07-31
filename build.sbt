name := "Scala2_11WithCats"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.2.0"
)

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
  "-Xlint", // enable handy linter warnings
  //"-Xfatal-warnings", // turn compiler warnings into errors
  "-Ypartial-unification" // allow the compiler to unify type constructors of different arities
)

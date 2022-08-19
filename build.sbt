import Dependencies._

// Flags
Global / semanticdbEnabled                      := true
Global / onChangedBuildSource                   := ReloadOnSourceChanges
Global / scalacOptions                          := Seq(
  "-Ywarn-unused:imports",
  "-Werror",
  "-feature",
  "-language:reflectiveCalls",
)
Global / scalaVersion                           := "2.13.8"
ThisBuild / versionScheme                       := Some("early-semver")
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / githubWorkflowPublish               := Seq(WorkflowStep.Sbt(List("ci-release")))
ThisBuild / githubWorkflowPublishTargetBranches += RefPredicate.StartsWith(Ref.Tag("v"))
ThisBuild / githubWorkflowPublish               := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_PASSPHRASE"    -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET"        -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}",
    ),
  ),
)

inThisBuild(
  List(
    organization := "com.tusharmath",
    homepage     := Some(url("https://github.com/tusharmath/zio-compose")),
    licenses     := List("MIT" -> url("https://github.com/tusharmath/zio-compose/blob/main/LICENSE")),
    developers   := List(
      Developer(
        "tusharmath",
        "Tushar Mathur",
        "tusharmath@gmail.com",
        url("https://tusharmath.com"),
      ),
    ),
  ),
)

// Projects
lazy val root = (project in file("."))
  .aggregate(zioCompose, zioComposeMacros, zioComposeExamples)
  .settings(name := "root", publish / skip := true)

lazy val zioCompose = project
  .in(file("./compose"))
  .settings(
    name                := "zio-compose",
    libraryDependencies := Seq(
      ZIOCore,
      ZIOSchema,
      ZIOSchemaJson,
      ZIOSchemaDerivation,
      ZIOTest,
      ZIOTestSbt,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )
  .dependsOn(zioComposeMacros)

lazy val zioComposeMacros = project
  .in(file("./compose-macros"))
  .settings(
    name                := "zio-compose-macros",
    libraryDependencies := Seq(
      ZIOSchema,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
    ),
  )

lazy val zioComposeExamples = project
  .in(file("./compose-examples"))
  .dependsOn(zioCompose, zioComposeMacros)
  .settings(
    name           := "zio-compose-examples",
    publish / skip := true,
    fork           := false,
  )

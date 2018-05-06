import com.typesafe.sbt.SbtScalariform._

name := "msc"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.4"

scalacOptions ++= Seq(
  "-Ypartial-unification",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint:-unused,_",
  "-Ywarn-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-inaccessible",
  "-Ywarn-nullary-override"
)

resolvers ++= Seq(
  Resolver.jcenterRepo,
  Resolver.sonatypeRepo("snapshots")
)

def akka(module: String, version: String = "2.5.12") = "com.typesafe.akka" %% s"akka-$module" % version

libraryDependencies ++= Seq(
  ehcache,
  guice,
  filters,
  ws,

  akka("persistence"),
  akka("persistence-query"),
  akka("persistence-cassandra", version = "0.84"),
  akka("actor"),
  akka("http-core", version = "10.1.1"),
  akka("slf4j"),

  "com.mohiva"             %% "play-silhouette"                 % "5.0.0",
  "com.mohiva"             %% "play-silhouette-password-bcrypt" % "5.0.0",
  "com.mohiva"             %% "play-silhouette-persistence"     % "5.0.0",
  "com.mohiva"             %% "play-silhouette-crypto-jca"      % "5.0.0",

  "net.codingwell"         %% "scala-guice"                     % "4.1.0",
  "com.iheart"             %% "ficus"                           % "1.4.1",
  "com.enragedginger"      %% "akka-quartz-scheduler"           % "1.6.1-akka-2.5.x",
  "com.adrianhurt"         %% "play-bootstrap"                  % "1.2-P26-B3",

  "com.typesafe.play"      %% "play-slick"                      % "3.0.3",
  "com.typesafe.play"      %% "play-slick-evolutions"           % "3.0.3",
  "com.h2database"         %  "h2"                              % "1.4.196",
  "org.postgresql"         %  "postgresql"                       % "42.2.2",
  "org.typelevel"          %% "cats-core"                       % "1.0.1",

  "com.github.javafaker"   %  "javafaker"                       % "0.14",

  "org.scalatest"          %% "scalatest"                       % "3.0.4" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play"              % "3.1.0" % Test
)



enablePlugins(PlayScala)
enablePlugins(SbtWeb)
enablePlugins(LauncherJarPlugin)
enablePlugins(ElasticBeanstalkPlugin)

maintainer in Docker := "Łukasz Wawrzyk <lukasz.wawrzyk@gmail.com>"
dockerExposedPorts := Seq(9000)
dockerBaseImage := "java:8"

routesGenerator := InjectedRoutesGenerator
routesImport ++= Seq(
  "pl.edu.agh.msc.utils.Binders._",
  "pl.edu.agh.msc.products.ProductId",
  "pl.edu.agh.msc.orders.OrderId",
  "pl.edu.agh.msc.payment.PaymentId",
)
TwirlKeys.templateImports := Seq()

pipelineStages := Seq(gzip)

sources in (Compile,doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false

parallelExecution in Test := false

ScalariformKeys.preferences := {
  import scalariform.formatter.preferences._

  ScalariformKeys.preferences.value
    .setPreference(FormatXml, false)
    .setPreference(DoubleIndentConstructorArguments, false)
    .setPreference(DanglingCloseParenthesis, Preserve)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(AlignArguments, true)
    .setPreference(AlignParameters, true)
    .setPreference(DanglingCloseParenthesis, Force)
    .setPreference(PreserveSpaceBeforeArguments, true)
    .setPreference(SpacesAroundMultiImports, true)
}

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
  "-Ywarn-nullary-override",
  "-Ywarn-numeric-widen"
)

resolvers ++= Seq(
  Resolver.jcenterRepo,
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  ehcache,
  guice,
  filters,

  "com.mohiva"             %% "play-silhouette"                 % "5.0.0",
  "com.mohiva"             %% "play-silhouette-password-bcrypt" % "5.0.0",
  "com.mohiva"             %% "play-silhouette-persistence"     % "5.0.0",
  "com.mohiva"             %% "play-silhouette-crypto-jca"      % "5.0.0",

  "org.webjars"            %% "webjars-play"                    % "2.6.1",
  "org.webjars"            %  "bootstrap"                       % "3.3.7-1" exclude("org.webjars", "jquery"),
  "org.webjars"            %  "jquery"                          % "3.2.1",
  "net.codingwell"         %% "scala-guice"                     % "4.1.0",
  "com.iheart"             %% "ficus"                           % "1.4.1",
  "com.enragedginger"      %% "akka-quartz-scheduler"           % "1.6.1-akka-2.5.x",
  "com.adrianhurt"         %% "play-bootstrap"                  % "1.2-P26-B3",

  "com.typesafe.play"      %% "play-slick"                      % "3.0.3",
  "com.typesafe.play"      %% "play-slick-evolutions"           % "3.0.3",
  "com.h2database"         %  "h2"                              % "1.4.196",
  "org.typelevel"          %% "cats-core"                       % "1.0.1",

  "com.github.javafaker"   %  "javafaker"                       % "0.14",

  "org.scalatest"          %% "scalatest"                       % "3.0.4" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play"              % "3.1.0" % Test
)



enablePlugins(PlayScala)

routesGenerator := InjectedRoutesGenerator
routesImport ++= Seq(
  "pl.edu.agh.msc.utils.Binders._",
  "pl.edu.agh.msc.products.ProductId"
)
TwirlKeys.templateImports := Seq()

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

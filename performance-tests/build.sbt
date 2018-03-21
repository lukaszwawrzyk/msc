name := "msc-perftests"

scalaVersion := "2.12.4"

version := "0.0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.3.0" % Test,
  "io.gatling"            % "gatling-test-framework"    % "2.3.0" % Test,
  "com.github.javafaker"  % "javafaker"                 % "0.14"  % Test
)

enablePlugins(GatlingPlugin)
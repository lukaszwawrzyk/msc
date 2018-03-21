name := "msc-perftests"

scalaVersion := "2.12.4"

version := "0.0.1-SNAPSHOT"

scalaSource in Gatling := baseDirectory.value / "user-files" / "simulations"

libraryDependencies ++= Seq(
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.3.0" % "test",
  "io.gatling"            % "gatling-test-framework"    % "2.3.0" % "test"
)

enablePlugins(GatlingPlugin)
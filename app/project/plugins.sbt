resolvers += Resolver.url("bintray-kipsigman-sbt-plugins", url("http://dl.bintray.com/kipsigman/sbt-plugins"))(Resolver.ivyStylePatterns)

addSbtPlugin("kipsigman" % "sbt-elastic-beanstalk" % "0.1.4-SNAPSHOT")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.11")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.2")
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.10")
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.3")
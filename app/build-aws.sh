sbt elastic-beanstalk:dist
sbt docker:publishLocal
sed -i 's!USER daemon!USER daemon\nRUN [\"chmod\", \"+x\", \"/opt/docker/bin/msc\"]!g' target/docker/stage/Dockerfile
sbt elastic-beanstalk:dist

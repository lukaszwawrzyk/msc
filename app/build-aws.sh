sbt elastic-beanstalk:dist
cp Dockerfile target/docker/stage/Dockerfile
sbt elastic-beanstalk:dist

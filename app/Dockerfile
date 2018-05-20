FROM java:8
LABEL MAINTAINER="Łukasz Wawrzyk <lukasz.wawrzyk@gmail.com>"
WORKDIR /opt/docker
ADD --chown=daemon:daemon opt /opt
EXPOSE 9000
EXPOSE 2552
EXPOSE 8558
USER daemon
RUN echo "export PUB_IP=\`curl -s http://169.254.169.254/latest/meta-data/public-ipv4\`" > "/opt/docker/bin/run" && \
    echo "export PRV_IP=\`curl -s http://169.254.169.254/latest/meta-data/local-ipv4\`" >> "/opt/docker/bin/run" && \
    echo "/opt/docker/bin/msc" >> "/opt/docker/bin/run"
RUN ["chmod", "+x", "/opt/docker/bin/msc", "/opt/docker/bin/run"]
CMD /opt/docker/bin/run
FROM openjdk:8-jre-alpine

LABEL vendor="AVAYA Inc"
LABEL maintainer="EPT - Emerging Products and Technologies"
LABEL com.avaya.ama.version="${project.version}"

ENV SOCIAL_DATA_DIRECTORY /socialData/fbRt
CMD mkdir ${SOCIAL_DATA_DIRECTORY}
VOLUME ${SOCIAL_DATA_DIRECTORY}

ADD maven/docker_log4j.xml /log4j.xml
# in DB is configured this following location
ADD maven/www_slateai_com.jks /config/messagingGateway/www_slateai_com.jks
ADD maven/${project.artifactId}-${project.version}.jar ${project.artifactId}.jar

EXPOSE 5056

ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -Dlog4j.configuration=file:/log4j.xml \
    -DdbConfigFileName \
    -jar /${project.artifactId}.jar \
    --messagingGateway.awsInterface.awsSqsInterface.zoneId=${ZONE} \
    --dbInterface.serverAddress=${DB_HOST} \
    --dbInterface.portNumber=${DB_PORT} \
    --dbInterface.userName=${DB_USERNAME} \
    --dbInterface.password=${DB_PASSWORD} \
    --dbInterface.deployId=${ZONE}

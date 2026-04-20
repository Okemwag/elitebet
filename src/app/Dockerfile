FROM amazoncorretto:21.0.1

EXPOSE 8080 5005

ARG JAR_FILE=./*.jar

ADD ${JAR_FILE} rentrow-api.jar

CMD java $JAVA_OPTS -jar rentrow-api.jar
FROM openjdk:17
ADD ./target/analyzer-0.0.1-SNAPSHOT.jar analyzer.jar
ENTRYPOINT ["java", "-jar", "analyzer.jar"]
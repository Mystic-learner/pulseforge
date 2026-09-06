FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY DemoService.java .
RUN javac DemoService.java
EXPOSE 8081
CMD ["java","DemoService"]

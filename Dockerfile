# Use the official Maven image to build the project
FROM maven:3.8.5-openjdk-21 AS build

# Set the working directory in the container
WORKDIR /kalbum

# Copy the project's pom.xml file to the container
COPY pom.xml .

# Download the dependencies
RUN mvn dependency:go-offline

# Copy the rest of the project files to the container
COPY src /kalbum/src

# Package the application
RUN mvn package -DskipTests

# Use the official OpenJDK image to run the application
FROM openjdk:21-jdk

# Set the working directory in the container
WORKDIR /kalbum

# Copy the packaged jar file from the build stage
COPY --from=build /kalbum/target/kalbum.jar kalbum.jar

# Specify the command to run the application
CMD ["java", "-jar", "kalbum.jar"]

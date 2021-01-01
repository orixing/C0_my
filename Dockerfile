FROM openjdk:12-alpine
COPY ./* /app/
WORKDIR /app/
RUN javac -d ./output ./App.java
WORKDIR /app/output
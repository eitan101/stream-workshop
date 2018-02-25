FROM anapsix/alpine-java:9

COPY target/stream-api-1.0-SNAPSHOT.jar /app/app.jar
COPY conf.yaml /app/conf.yaml

CMD ["java","-jar","/app/app.jar","server","/app/conf.yaml"]

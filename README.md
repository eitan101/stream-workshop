# stream-workshop

```sh
git clone https://github.com/eitan101/stream-workshop
cd stream-workshop/sse
docker run --rm -v $PWD:/my -w /my maven:3-jdk-9-slim mvn -q package
docker-compose up
```

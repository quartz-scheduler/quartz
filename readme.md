## Quartz Scheduler

Quartz is a richly featured, open source job scheduling library that can be integrated within virtually any Java application - from the smallest stand-alone application to the largest e-commerce system.

## Builld Status

Branch: `quartz-2.4.x`

![quartz-2.4.x branch build status](https://travis-ci.org/quartz-scheduler/quartz.svg?branch=quartz-2.4.x "quartz-2.4.x build status")


## Build Instructions

### Requirement

JDK 1.8

NOTE: You should use our maven wrapper (`./mvnw`) script to
get a more consistent build result. It should auto
download the correct version of Maven tool if you do not already
have one.


### To compile:
```
export JAVA_HOME=/path/to/jdk8
./mvnw compile
```


### To build distribution package

```
./mvnw package

# Or if you want to skip tests
./mvnw package -DskipTests
```

NOTE: The final combined single jar is found under  `quartz/target/quartz-<version>.jar`




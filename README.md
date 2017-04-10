
## Builld status: 
![Master branch build status](https://travis-ci.org/quartz-scheduler/quartz.svg?branch=master "Master build status")


## Build instructions:

### To compile:
```
  %> ./mvnw install -DskipTests
```

Note:  the final Quartz jar is found under quartz/target 

### To build Quartz distribution kit:
```
  %> cd distribution
  %> ./mvnw package
```


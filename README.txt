1. To compile:
  %> ./mvnw install -DskipTests
  
Note:  the final Quartz jar is found under quartz/target 

2. To build Quartz distribution kit:

  %> cd distribution
  %> ./mvnw package

3. To deploy Maven central repo (via Sonatype)

  %> ./mvnw clean deploy -P sign-artifacts,deploy-sonatype -DskipTests

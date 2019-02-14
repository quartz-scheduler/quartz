## quartz-2.3.x

THIS RELEASE REQUIRES JDK7

* #294 depen: Update hikaricp-java6:2.3.13 to hikaricp-java7:2.4.13
* #147 bugfix: Fix BINARY to BLOG type for job data for hsqldb
* #156 bugfix: Fix null string used in thread name with DirectSchedulerFactory
* #159 bugfix: Fix extra bad char tick on drop table qurtz_fired_triggers for postgres
* #146 bugfix: Release BLOCKED triggers in releaseAcquiredTrigger
* #212 bugfix: QuartzInitializerListener: fix a typo
* #193 bugfix: Job execution context impl returns incorrect recovering job key
* #172 bugfix: Miss notify SchedulerListeners in QuartzScheduler.java
* #160 improv: Add drop table if exists check in sql script for postgres
* #214 improv: Reuse JobBuilder.storeDurably(boolean) in JobBuilder
* #281 improv: Fix no setter for dataSource property validateOnCheckout
* #264 improv: Fix no setter for dataSource property discardIdleConnectionsSeconds
* #245 improv: Sybase: Changed varchar length TRIGGER_NAME from 80 to 200
* #340 improv: Use all-caps table names in the liquibase script
* #293 build: Setup Azure CI server for Quartz project
* #66  build: Remove unused 'svn' requirement during maven package build
* #301 build: Improve project with readme, and license changes log
* #302 build: Update mvnw wrapper to use Maven 3.6.0
* #226 build: Replace maven-forge-plugin with maven-jar-plugin
* #170 docs: Minor fix and improvement on Javadoc
* #203 docs: Minor fix and improvement on Javadoc

## quartz-2.3.0

THIS RELEASE REQUIRES JDK7

* #0   build: Released on 19-Apr-2017
* #0   build: #9 Fix the Java 8 javadoc issue with 'doclint:none'
* #0   depen: Require minimum JDK7
* #6   bugfix: Fix cannot create tables in MySQL with InnoDB and UTF8mb4
* #93  bugfix: Fix the jobs recovering (on scheduler startup)
* #76  improv: Add missing foreign key for BLOB triggers table for ms sql server
* #114 improv: Add 'if exists' clause to drop tables command for postgres
* #25  feat: Add resetTriggerFromErrorState functionality
* #126 feat: Add support for hikari cp, upgrade c3p0 library, break static dependencies

## Previous Releases

```
quartz-2.2.3 	18-Apr-2016
quartz-2.2.2 	12-Oct-2015
quartz-2.2.1 	25-Sep-2013
quartz-2.2.0 	29-Jun-2013
quartz-2.1.7 	05-Mar-2013
quartz-2.1.6 	22-Feb-2013
quartz-2.1.5 	27-Apr-2012
quartz-1.8.1 	11-Jun-2010
quartz-1.8.0 	23-Apr-2010
quartz-1.7.3 	24-Feb-2010
quartz-1.7.2 	10-Feb-2010 
```

## Change Categories

build: Changes for build, release and project management etc 
depen: Changes for third-party dependencies update
bugfix: Changes for bug fix
impro: Changes for code improvement, refactoring and reformat etc
feat: Changes for new feature
docs: Changes for documentation only
test: Changes for tests only
 
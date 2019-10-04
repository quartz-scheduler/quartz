---
title: Cookbook
visible_title: "Quartz Cookbook"
active_sub_menu_id: site_mnu_docs_cookbook
---
<div class="secNavPanel"><a href=".">Contents</a> | <a href="ServletInitScheduler.html">&lsaquo;&nbsp;Prev</a> | <a href="DefineJobWithData.html">Next&nbsp;&rsaquo;</a></div>





# How-To: Using Multiple (Non-Clustered) Schedulers

Reasons you may want to do this:

+ For managing resources - e.g. if you have a mix of light-weight and heavy-weight jobs, then you may wish to have a scheduler with many threads to service the lightweight jobs and one with few threads to service the heavy-weight jobs, in order to keep your machines resources from being overwhelmed by running to many heavy-weight jobs concurrently.
+ To schedule jobs in one application, but have them execute within another (when using JDBC-JobStore).


Note that you can create as many schedulers as you like within any application, but they must have unique scheduler names (typically defined in the quartz.properties file).  This means that you'll need to have multiple properties files, which means that you'll need to specify them as you initialize the StdSchedulerFactory (as it only defaults to finding "quartz.properties").

If you run multiple schedulers they can of course all have distinct characteristics - e.g. one may use RAMJobStore and have 100 worker threads, and another may use JDBC-JobStore and have 20 worker threads.

<blockquote>
Never start (scheduler.start()) a non-clustered instance against the same set of database tables that any other instance with the same scheduler name is running (start()ed) against. You may get serious data corruption, and will definitely experience erratic behavior.
</blockquote>

### Example/Discussion Relating To Scheduling Jobs From One Application To Be Executed In Another Application

*This description/usage applies to JDBC-JobStore.  You may also want to look at RMI or JMX features to control a Scheduler in a remote process - which works for any JobStore. You may also be interested in the Terracotta Quartz Where features.*

Currently, If you want to have particular jobs run in a particular scheduler, then it needs to be a distinct scheduler - unless you use the Terracotta Quartz Where features.

Suppose you have an application "App A" that needs to schedule jobs (based on user input) that need to run either on the local process/machine "Machine A" (for simple jobs) or on a remote machine "Machine B" (for complex jobs).

It is possible within an application to instantiate two (or more) schedulers, and schedule jobs into both (or more) schedulers, and have only the jobs placed into one scheduler run on the local machine.  This is achieved by calling scheduler.start() on the scheduler(s) within the process where you want the jobs to execute.  Scheduler.start() causes the scheduler instance to start processing the jobs (i.e. start waiting for trigger fire times to arrive, and then executing the jobs).  However a non-started scheduler instance can still be used to schedule (and retrieve) jobs.

For example:

+ In "App A" create "Scheduler A" (with config that points it at database tables prefixed with "A"), and invoke start() on "Scheduler A". Now "Scheduler A" in "App A" will execute jobs scheduled by "Scheduler A" in "App A"
+ In "App A" create "Scheduler B" (with config that points it at database tables prefixed with "B"), and DO NOT invoke start() on "Scheduler B". Now "Scheduler B" in "App A" can schedule jobs to be ran where "Scheduler B" is started.
+ In "App B" create "Scheduler B" (with config that points it at database tables prefixed with "B"), and invoke start() on "Scheduler B". Now "Scheduler B" in "App B" will execute jobs scheduled by "Scheduler B" in "App A".

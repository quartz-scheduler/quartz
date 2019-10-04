---
title: Cookbook
visible_title: "Quartz Cookbook"
active_sub_menu_id: site_mnu_docs_cookbook
---
<div class="secNavPanel"><a href=".">Contents</a> | <a href="UpdateTrigger.html">&lsaquo;&nbsp;Prev</a> | <a href="ListJobs.html">Next&nbsp;&rsaquo;</a></div>





# How-To: Initializing Job Data With Scheduler Initialization

You can initialize the scheduler with predefined jobs and triggers using the XMLSchedulingDataProcessorPlugin (which, with the 1.8 release, replaced the older JobInitializationPlugin). An example is provided in the Quartz distribution in the directory examples/example10. However, following is a short description of how the plugin works.

First of all, we need to explicitly specify in the scheduler properties that we want to use the XMLSchedulingDataProcessorPlugin. This is an excerpt from an example quartz.properties:

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
#===================================================
# Configure the Job Initialization Plugin
#===================================================

org.quartz.plugin.jobInitializer.class = org.quartz.plugins.xml.XMLSchedulingDataProcessorPlugin
org.quartz.plugin.jobInitializer.fileNames = jobs.xml
org.quartz.plugin.jobInitializer.failOnFileNotFound = true
org.quartz.plugin.jobInitializer.scanInterval = 10
org.quartz.plugin.jobInitializer.wrapInUserTransaction = false
</code></pre>


Let's see what each property does:

+ **fileNames**: a comma separated list of filenames (with paths). These files contain the xml definition of jobs and associated triggers. We'll see an example jobs.xml definition shortly.
+ **failOnFileNotFound**: if the xml definition files are not found, should the plugin throw an exception, thus preventing itself (the plugin) from initializing?
+ **scanInterval**: the xml definition files can be reloaded if a file change is detected. This is the interval (in seconds) the files are looked at. Set to 0 to disable scanning.
+ **wrapInUserTransaction**: if using the XMLSchedulingDataProcessorPlugin with JobStoreCMT, be sure to set the value of this property to true, otherwise you might experience unexpected behavior.



The jobs.xml file (or any other name you use for it in the fileNames property) declaratively defines jobs and triggers. It can also contain directive to delete existing data.  Here's a self-explanatory example:


<pre class="prettyprint highlight"><code class="language-xml" data-lang="xml">
&lt;?xml version='1.0' encoding='utf-8'?&gt;
&lt;job-scheduling-data xmlns="http://www.quartz-scheduler.org/xml/JobSchedulingData"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.quartz-scheduler.org/xml/JobSchedulingData http://www.quartz-scheduler.org/xml/job_scheduling_data_1_8.xsd"
  version="1.8"&gt;

    &lt;schedule&gt;
        &lt;job&gt;
            &lt;name&gt;my-very-clever-job&lt;/name&gt;
            &lt;group&gt;MYJOB_GROUP&lt;/group&gt;

            &lt;description&gt;The job description&lt;/description&gt;
            &lt;job-class&gt;com.acme.scheduler.job.CleverJob&lt;/job-class&gt;
            &lt;job-data-map allows-transient-data="false"&gt;

                &lt;entry&gt;
                    &lt;key&gt;burger-type&lt;/key&gt;
                    &lt;value&gt;hotdog&lt;/value&gt;
                &lt;/entry&gt;
                &lt;entry&gt;

                    &lt;key&gt;dressing-list&lt;/key&gt;
                    &lt;value&gt;ketchup,mayo&lt;/value&gt;
                &lt;/entry&gt;
            &lt;/job-data-map&gt;
        &lt;/job&gt;

        &lt;trigger&gt;
            &lt;cron&gt;
                &lt;name&gt;my-trigger&lt;/name&gt;
                &lt;group&gt;MYTRIGGER_GROUP&lt;/group&gt;
                &lt;job-name&gt;my-very-clever-job&lt;/job-name&gt;

                &lt;job-group&gt;MYJOB_GROUP&lt;/job-group&gt;
                &lt;!-- trigger every night at 4:30 am --&gt;
                &lt;!-- do not forget to light the kitchen's light --&gt;
                &lt;cron-expression&gt;0 30 4 * * ?&lt;/cron-expression&gt;

            &lt;/cron&gt;
        &lt;/trigger&gt;
    &lt;/schedule&gt;
&lt;/job-scheduling-data&gt;
</code></pre>


A further jobs.xml example is in the examples/example10 directory of the Quartz distribution.

Checkout the <a href="http://www.quartz-scheduler.org/xml/job_scheduling_data_2_0.xsd">XML schema</a> for full details of what is possible.

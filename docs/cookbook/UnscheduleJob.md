---
title: Cookbook
visible_title: "Quartz Cookbook"
active_sub_menu_id: site_mnu_docs_cookbook
---
<div class="secNavPanel"><a href=".">Contents</a> | <a href="ScheduleJob.md">&lsaquo;&nbsp;Prev</a> | <a href="StoreJob.md">Next&nbsp;&rsaquo;</a></div>





# How-To: Unscheduling a Job

### Unscheduling a Particular Trigger of Job

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
// Unschedule a particular trigger from the job (a job may have more than one trigger)
scheduler.unscheduleJob(triggerKey("trigger1", "group1"));
</code></pre>


### Deleting a Job and Unscheduling All of Its Triggers

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
// Schedule the job with the trigger
scheduler.deleteJob(jobKey("job1", "group1"));
</code></pre>

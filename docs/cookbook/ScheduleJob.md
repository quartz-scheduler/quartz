---
title: Cookbook
visible_title: "Quartz Cookbook"
active_sub_menu_id: site_mnu_docs_cookbook
---
<div class="secNavPanel"><a href=".">Contents</a> | <a href="DefineJobWithData.html">&lsaquo;&nbsp;Prev</a> | <a href="UnscheduleJob.html">Next&nbsp;&rsaquo;</a></div>





# How-To: Scheduling a Job

### Scheduling a Job

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
// Define job instance
JobDetail job1 = JobBuilder.newJob(ColorJob.class)
    .withIdentity("job1", "group1")
    .build();

// Define a Trigger that will fire "now", and not repeat
Trigger trigger = TriggerBuilder.newTrigger()
    .withIdentity("trigger1", "group1")
    .startNow()
    .build();

// Schedule the job with the trigger
sched.scheduleJob(job, trigger);
</code></pre>

---
title: Cookbook
visible_title: "Quartz Cookbook"
active_sub_menu_id: site_mnu_docs_cookbook
---
<div class="secNavPanel"><a href=".">Contents</a> | <a href="StoreJob.html">&lsaquo;&nbsp;Prev</a> | <a href="UpdateJob.html">Next&nbsp;&rsaquo;</a></div>





# How-To: Scheduling an already stored job

### Scheduling an already stored job

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
// Define a Trigger that will fire "now" and associate it with the existing job
Trigger trigger = newTrigger()
    .withIdentity("trigger1", "group1")
    .startNow()
    .forJob(jobKey("job1", "group1"))
    .build();

// Schedule the trigger
sched.scheduleJob(trigger);
</code></pre>

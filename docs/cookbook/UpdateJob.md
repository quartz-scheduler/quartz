---
title: Cookbook
visible_title: "Quartz Cookbook"
active_sub_menu_id: site_mnu_docs_cookbook
---
<div class="secNavPanel"><a href=".">Contents</a> | <a href="ScheduleStoredJob.html">&lsaquo;&nbsp;Prev</a> | <a href="UpdateTrigger.html">Next&nbsp;&rsaquo;</a></div>





# How-To: Update an existing job

### Update an existing job

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
// Add the new job to the scheduler, instructing it to "replace"
//  the existing job with the given name and group (if any)
JobDetail job1 = newJob(MyJobClass.class)
    .withIdentity("job1", "group1")
    .build();

// store, and set overwrite flag to 'true'     
scheduler.addJob(job1, true);

</code></pre>

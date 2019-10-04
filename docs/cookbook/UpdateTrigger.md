---
title: Cookbook
visible_title: "Quartz Cookbook"
active_sub_menu_id: site_mnu_docs_cookbook
---
<div class="secNavPanel"><a href=".">Contents</a> | <a href="UpdateJob.html">&lsaquo;&nbsp;Prev</a> | <a href="JobInitPlugin.html">Next&nbsp;&rsaquo;</a></div>





# How-To: Updating a trigger

### Replacing a trigger

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
// Define a new Trigger
Trigger trigger = newTrigger()
    .withIdentity("newTrigger", "group1")
    .startNow()
    .build();

// tell the scheduler to remove the old trigger with the given key, and put the new one in its place
sched.rescheduleJob(triggerKey("oldTrigger", "group1"), trigger);

</code></pre>


### Updating an existing trigger

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
// retrieve the trigger
Trigger oldTrigger = sched.getTrigger(triggerKey("oldTrigger", "group1");

// obtain a builder that would produce the trigger
TriggerBuilder tb = oldTrigger.getTriggerBuilder();

// update the schedule associated with the builder, and build the new trigger
// (other builder methods could be called, to change the trigger in any desired way)
Trigger newTrigger = tb.withSchedule(simpleSchedule()
    .withIntervalInSeconds(10)
    .withRepeatCount(10)
    .build();

sched.rescheduleJob(oldTrigger.getKey(), newTrigger);

</code></pre>

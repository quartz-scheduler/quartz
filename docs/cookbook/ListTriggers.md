---
title: Cookbook
visible_title: "Quartz Cookbook"
active_sub_menu_id: site_mnu_docs_cookbook
---
<div class="secNavPanel"><a href=".">Contents</a> | <a href="ListJobs.html">&lsaquo;&nbsp;Prev</a> | <a href="JobTriggers.html">Next&nbsp;&rsaquo;</a></div>





# How-To: Listing Triggers In Scheduler

### Listing all Triggers in the scheduler

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
// enumerate each trigger group
for(String group: sched.getTriggerGroupNames()) {
    // enumerate each trigger in group
    for(TriggerKey triggerKey : sched.getTriggerKeys(groupEquals(group))) {
        System.out.println("Found trigger identified by: " + triggerKey);
    }
}
</code></pre>

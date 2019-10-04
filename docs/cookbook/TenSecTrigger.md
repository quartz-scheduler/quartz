---
title: Cookbook
visible_title: "Quartz Cookbook"
active_sub_menu_id: site_mnu_docs_cookbook
---
<div class="secNavPanel"><a href=".">Contents</a> | <a href="SchedulerListeners.html">&lsaquo;&nbsp;Prev</a> | <a href="NintyMinTrigger.html">Next&nbsp;&rsaquo;</a></div>





# How-To: Trigger That Executes Every Ten Seconds

### Using SimpleTrigger


<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
trigger = newTrigger()
    .withIdentity("trigger3", "group1")
    .startNow()
    .withSchedule(simpleSchedule()
            .withIntervalInSeconds(10)
            .repeatForever())
    .build();
</code></pre>

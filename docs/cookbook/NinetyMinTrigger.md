---
title: Cookbook
visible_title: "Quartz Cookbook"
active_sub_menu_id: site_mnu_docs_cookbook
---
<div class="secNavPanel"><a href=".">Contents</a> | <a href="TenSecTrigger.html">&lsaquo;&nbsp;Prev</a> | <a href="DailyTrigger.html">Next&nbsp;&rsaquo;</a></div>





# How-To: Trigger That Executes Every 90 minutes

### Using SimpleTrigger


<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
trigger = newTrigger()
    .withIdentity("trigger3", "group1")
    .startNow()
    .withSchedule(simpleSchedule()
            .withIntervalInMinutes(90)
            .repeatForever())
    .build();
</code></pre>


### Using CalendarIntervalTrigger


<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
trigger = newTrigger()
    .withIdentity("trigger3", "group1")
    .startNow()
    .withSchedule(calendarIntervalSchedule()
            .withIntervalInMinutes(90))
    .build();
</code></pre>

---
title: Cookbook
visible_title: "Quartz Cookbook"
active_sub_menu_id: site_mnu_docs_cookbook
---
<div class="secNavPanel"><a href=".">Contents</a> | <a href="CreateScheduler.html">&lsaquo;&nbsp;Prev</a> | <a href="ShutdownScheduler.html">Next&nbsp;&rsaquo;</a></div>





# How-To: Placing a Scheduler in Stand-by Mode

### Placing a Scheduler in Stand-by Mode

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
// start() was previously invoked on the scheduler

scheduler.standby();

// now the scheduler will not fire triggers / execute jobs

// ...

scheduler.start();

// now the scheduler will fire triggers and execute jobs
</code></pre>

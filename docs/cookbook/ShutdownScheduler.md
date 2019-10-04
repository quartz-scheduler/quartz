---
title: Cookbook
visible_title: "Quartz Cookbook"
active_sub_menu_id: site_mnu_docs_cookbook
---
<div class="secNavPanel"><a href=".">Contents</a> | <a href="SchedulerStandby.html">&lsaquo;&nbsp;Prev</a> | <a href="ServletInitScheduler.html">Next&nbsp;&rsaquo;</a></div>





# How-To: Shutting Down a Scheduler

To shutdown / destroy a scheduler, simply call one of the shutdown(..) methods.

Once you have shutdown a scheduler, it cannot be restarted (as threads and other resources are permanently destroyed). Also see the suspend method if you wish to simply pause the scheduler for a while.


### Wait for Executing Jobs to Finish

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
//shutdown() does not return until executing Jobs complete execution
scheduler.shutdown(true);
</code></pre>


### Do Not Wait for Executing Jobs to Finish

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
//shutdown() returns immediately, but executing Jobs continue running to completion
scheduler.shutdown();
//or
scheduler.shutdown(false);
</code></pre>


If you are using the `org.quartz.ee.servlet.QuartzInitializerListener` to fire up a scheduler in your servlet container, its `contextDestroyed()` method will shutdown the scheduler when your application is undeployed or the application server shuts down (unless its shutdown-on-unload property has been explicitly set to false).

---
title: Cookbook
visible_title: "Quartz Cookbook"
active_sub_menu_id: site_mnu_docs_cookbook
---
<div class="secNavPanel"><a href=".">Contents</a> | <a href="TriggerListeners.html">&lsaquo;&nbsp;Prev</a> | <a href="TenSecTrigger.html">Next&nbsp;&rsaquo;</a></div>





# How-To: Using Scheduler Listeners

### Creating a SchedulerListener

Extend TriggerListenerSupport and override methods for events you're interested in.

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
package foo;

import org.quartz.Trigger;
import org.quartz.listeners.SchedulerListenerSupport;

public class MyOtherSchedulerListener extends SchedulerListenerSupport {

    @Override
    public void schedulerStarted() {
        // do something with the event
    }

    @Override
    public void schedulerShutdown() {
        // do something with the event
    }

    @Override
    public void jobScheduled(Trigger trigger) {
        // do something with the event
    }

}
</code></pre>


### Registering A SchedulerListener With The Scheduler


<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
scheduler.getListenerManager().addSchedulerListener(mySchedListener);
</code></pre>

---
title: Cookbook
visible_title: "Quartz Cookbook"
active_sub_menu_id: site_mnu_docs_cookbook
---
<div class="secNavPanel"><a href=".">Contents</a> | <a href="JobListeners.html">&lsaquo;&nbsp;Prev</a> | <a href="SchedulerListeners.html">Next&nbsp;&rsaquo;</a></div>





# How-To: Using Trigger Listeners

### Creating a TriggerListener

Implement the TriggerListener interface.

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
package foo;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.Trigger.CompletedExecutionInstruction;

public class MyTriggerListener implements TriggerListener {

    private String name;

    public MyTriggerListener(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void triggerComplete(Trigger trigger, JobExecutionContext context,
            CompletedExecutionInstruction triggerInstructionCode) {
        // do something with the event

    }

    public void triggerFired(Trigger trigger, JobExecutionContext context) {
        // do something with the event
    }

    public void triggerMisfired(Trigger trigger) {
        // do something with the event
    }

    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        // do something with the event
        return false;
    }

}
</code></pre>


OR -

Extend TriggerListenerSupport.

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
package foo;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;

public class MyOtherTriggerListener extends TriggerListenerSupport {

    private String name;

    public MyOtherTriggerListener(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {
        // do something with the event
    }
}
</code></pre>


### Registering A TriggerListener With The Scheduler To Listen To All Triggers


<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
scheduler.getListenerManager().addTriggerListener(myTriggerListener, allTriggers());
</code></pre>


### Registering A TriggerListener With The Scheduler To Listen To A Specific Trigger


<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
scheduler.getListenerManager().addTriggerListener(myTriggerListener, triggerKeyEquals(triggerKey("myTriggerName", "myTriggerGroup")));
</code></pre>


### Registering A TriggerListener With The Scheduler To Listen To All Triggers In a Group


<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
scheduler.getListenerManager().addTriggerListener(myTriggerListener, triggerGroupEquals("myTriggerGroup"));
</code></pre>

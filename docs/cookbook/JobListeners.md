---
title: Cookbook
visible_title: "Quartz Cookbook"
active_sub_menu_id: site_mnu_docs_cookbook
---
<div class="secNavPanel"><a href=".">Contents</a> | <a href="JobTriggers.html">&lsaquo;&nbsp;Prev</a> | <a href="TriggerListeners.html">Next&nbsp;&rsaquo;</a></div>





# How-To: Using Job Listeners

### Creating a JobListener

Implement the JobListener interface.

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
package foo;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

public class MyJobListener implements JobListener {

    private String name;

    public MyJobListener(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void jobToBeExecuted(JobExecutionContext context) {
        // do something with the event
    }

    public void jobWasExecuted(JobExecutionContext context,
            JobExecutionException jobException) {
        // do something with the event
    }

    public void jobExecutionVetoed(JobExecutionContext context) {
        // do something with the event
    }
}
</code></pre>


OR -

Extend JobListenerSupport.

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
package foo;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;

public class MyOtherJobListener extends JobListenerSupport {

    private String name;

    public MyOtherJobListener(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
        @Override
    public void jobWasExecuted(JobExecutionContext context,
            JobExecutionException jobException) {
        // do something with the event
    }
}
</code></pre>


### Registering A JobListener With The Scheduler To Listen To All Jobs


<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
scheduler.getListenerManager().addJobListener(myJobListener, allJobs());
</code></pre>


### Registering A JobListener With The Scheduler To Listen To A Specific Job


<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
scheduler.getListenerManager().addJobListener(myJobListener, jobKeyEquals(jobKey("myJobName", "myJobGroup")));
</code></pre>


### Registering A JobListener With The Scheduler To Listen To All Jobs In a Group


<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
scheduler.getListenerManager().addJobListener(myJobListener, jobGroupEquals("myJobGroup"));
</code></pre>

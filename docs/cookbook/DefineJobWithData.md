---
title: Cookbook
visible_title: "Quartz Cookbook"
active_sub_menu_id: site_mnu_docs_cookbook
---
<div class="secNavPanel"><a href=".">Contents</a> | <a href="MultipleSchedulers.html">&lsaquo;&nbsp;Prev</a> | <a href="ScheduleJob.html">Next&nbsp;&rsaquo;</a></div>





# How-To: Defining a Job (with input data)


### A Job Class

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
public class PrintPropsJob implements Job {

	public PrintPropsJob() {
		// Instances of Job must have a public no-argument constructor.
	}

	public void execute(JobExecutionContext context)
			throws JobExecutionException {

		JobDataMap data = context.getMergedJobDataMap();
		System.out.println("someProp = " + data.getString("someProp"));
	}

}
</code></pre>

### Defining a Job Instance

<pre class="prettyprint highlight"><code class="language-java" data-lang="java">
// Define job instance
JobDetail job1 = JobBuilder.newJob(MyJobClass.class)
    .withIdentity("job1", "group1")
    .usingJobData("someProp", "someValue")
    .build();
</code></pre>


Also note that if your Job class contains setter methods that match your JobDataMap keys (e.g. "setSomeProp" for the
data in the above example), and you use the default JobFactory implementation, then Quartz will automatically call
the setter method with the JobDataMap value, and there is no need to have code in the Job's execute method that
retrieves the value from the JobDataMap.

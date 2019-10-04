/* 
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved. 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 * 
 */
package org.terracotta.quartz.tests;

import org.junit.Assert;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.calendar.BaseCalendar;
import org.terracotta.quartz.AbstractTerracottaJobStore;
import org.terracotta.tests.base.AbstractClientBase;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.terracotta.quartz.TerracottaJobStore;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class ShutdownClient extends AbstractClientBase {
  private static final List<WeakReference<ClassLoader>> CLASS_LOADER_LIST = new ArrayList<WeakReference<ClassLoader>>();

  public ShutdownClient(String[] args) {
    super(args);
  }

  public static void main(String[] args) throws Throwable {
    ShutdownClient client = new ShutdownClient(args);
    client.doTest();
  }

  @Override
  public void doTest() throws Throwable {
    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread thread, Throwable e) {
        threadDump(e);
        System.exit(-1);
      }
    });

    Collection<ThreadInfo> baselineThreads = getThreadDump();

    for (int i = 0; i < 5; i++) {
      System.out.println("***** Iteration " + (i + 1) + " *****");
      Scheduler myScheduler = setupScheduler();
      test(myScheduler);
      storeL1ClassLoaderWeakReferences(myScheduler);
      myScheduler.shutdown(true);

      System.runFinalization();
    }

    Collection<ThreadInfo> lingeringThreads = null;
    for (int i = 0; i < 30; i++) {
      lingeringThreads = getThreadDump();
      removeThreads(lingeringThreads, baselineThreads);
      removeKnownThreads(lingeringThreads);
      if (lingeringThreads.isEmpty()) {
        break;
      }
      TimeUnit.SECONDS.sleep(1);
    }

    if (!lingeringThreads.isEmpty()) {
      throw new AssertionError("Lingering Threads : " + lingeringThreads);
    }

    // let's trigger 10 full GC's to make sure the L1Loader got collected
    // let's also make sure another thread doesn't OOME while this thread stresses the perm gen
    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        if (!(e instanceof OutOfMemoryError)) {
          threadDump(e);
          System.exit(-1);
        }
        // else ignore
      }
    });
    for (int i = 0; i < 10; i++) {
      new PermStress().stress(100000);
    }

    assertClassloadersGCed();

    pass();
  }

  private Scheduler setupScheduler() throws IOException, SchedulerException {
    Properties props = new Properties();
    props.load(ShutdownClient.class.getResourceAsStream("/org/quartz/quartz.properties"));
    props.setProperty(StdSchedulerFactory.PROP_JOB_STORE_CLASS, TerracottaJobStore.class.getName());
    props.setProperty(AbstractTerracottaJobStore.TC_CONFIGURL_PROP, getTerracottaUrl());
    props.setProperty(StdSchedulerFactory.PROP_SCHED_INSTANCE_ID, StdSchedulerFactory.AUTO_GENERATE_INSTANCE_ID);

    SchedulerFactory schedFact = new StdSchedulerFactory(props);
    Scheduler sched = schedFact.getScheduler();
    sched.start();
    return sched;
  }

  private static void threadDump(final Throwable e) {
    System.out.println("Uncaught exception");
    System.out.println("----------------------------");
    e.printStackTrace(System.out);

    System.out.println("Generating Thread-dump at:" + (new java.util.Date()).toString());
    System.out.println("----------------------------");
    Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
    for (Thread t : map.keySet()) {
      StackTraceElement[] elem = map.get(t);
      System.out.print("\"" + t.getName() + "\"");
      System.out.print(" prio=" + t.getPriority());
      System.out.print(" tid=" + t.getId());
      Thread.State s = t.getState();
      String state = s.name();
      System.out.println(" @@@@ " + state);
      for (StackTraceElement anElem : elem) {
        System.out.print("  at ");
        System.out.println(anElem.toString());
      }
      System.out.println("----------------------------");
    }
  }

  // if only a single L1 loader got GC'ed, we can consider the test passed
  private static void assertClassloadersGCed() {
    boolean failed = true;
    StringBuilder sb = new StringBuilder();
    for (WeakReference<ClassLoader> wr : CLASS_LOADER_LIST) {
      ClassLoader cl = wr.get();
      if (cl != null) {
        sb.append(cl).append(", ");
      } else {
        failed = false;
      }
    }
    if (failed) {
      sb.deleteCharAt(sb.length() - 1);
      sb.deleteCharAt(sb.length() - 1);
      dumpHeap(ShutdownClient.class.getSimpleName());
      throw new AssertionError("Classloader(s) " + sb + " not GC'ed");
    }
  }

  private static void test(Scheduler scheduler) throws Throwable {
    JobDetailImpl jobDetail = new JobDetailImpl("testjob", null, SimpleJob.class);
    jobDetail.getJobDataMap().put("await-time", 150);
    jobDetail.setDurability(true);


    // This calendar doesn't do anything really, just testing that calendars work
    if (!scheduler.getCalendarNames().contains("mycal")) {
      scheduler.addCalendar("mycal", new BaseCalendar(), false, true);
    }
    if (!scheduler.checkExists(jobDetail.getKey())) {
      scheduler.addJob(jobDetail, false);
    }
    
    Trigger trigger = TriggerBuilder.newTrigger().forJob("testjob").modifiedByCalendar("mycal").build();
    scheduler.scheduleJob(trigger);
    SimpleJob.localBarrier.await();
  }

  private static Collection<ThreadInfo> getThreadDump() {
    Collection<ThreadInfo> dump = new ArrayList<ThreadInfo>();
    ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
    for (ThreadInfo tinfo : tbean.getThreadInfo(tbean.getAllThreadIds(), Integer.MAX_VALUE)) {
      if (tinfo != null) {
        dump.add(tinfo);
      }
    }
    return dump;
  }

  private static void removeThreads(Collection<ThreadInfo> from, Collection<ThreadInfo> remove) {
    for (Iterator<ThreadInfo> it = from.iterator(); it.hasNext();) {
      ThreadInfo ti = it.next();
      for (ThreadInfo r : remove) {
        if (r.getThreadId() == ti.getThreadId()) {
          it.remove();
          break;
        }
      }
    }
  }
  
  private static void removeKnownThreads(Collection<ThreadInfo> dump) {
    List<ThreadIgnore> ignores = Arrays.asList(new ThreadIgnore("http", "org.apache.tomcat."),
                                               new ThreadIgnore("Attach Listener"),
                                               new ThreadIgnore("Poller SunPKCS11", "sun.security.pkcs11."),
                                               new ThreadIgnore("(Attach Listener)"),
                                               new ThreadIgnore("JFR request timer"),
                                               new ThreadIgnore("JMAPI event thread"),
                                               new ThreadIgnore("AWT-AppKit"),
                                               new ThreadIgnore("Keep-Alive-SocketCleaner"));

    for (Iterator<ThreadInfo> it = dump.iterator(); it.hasNext();) {
      ThreadInfo threadInfo = it.next();
      for (ThreadIgnore ignore : ignores) {
        if (ignore.canIgnore(threadInfo)) {
          it.remove();
          break;
        }
      }
    }
  }

  private static void storeL1ClassLoaderWeakReferences(Scheduler scheduler) throws Exception {
    ClassLoader clusteredStateLoader = getUnderlyingToolkit(scheduler).getClass().getClassLoader();

    System.out.println("XXX: clusteredStateLoader: " + clusteredStateLoader);
    Assert.assertNotNull(clusteredStateLoader);

    CLASS_LOADER_LIST.add(new WeakReference<ClassLoader>(clusteredStateLoader));
  }

  private static Object getUnderlyingToolkit(Scheduler scheduler) throws Exception {
    Object sched = getPrivateFieldValue(scheduler, "sched");
    Object resources = getPrivateFieldValue(sched, "resources");
    Object jobStore = getPrivateFieldValue(resources, "jobStore");
    Object realJobStore = getPrivateFieldValue(AbstractTerracottaJobStore.class, jobStore, "toolkit");
    return realJobStore;
  }

  private static Object getPrivateFieldValue(Object target, String fieldName) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(target);
  }

  private static Object getPrivateFieldValue(Class clazz, Object target, String fieldName) throws Exception {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(target);
  }

  private static void dumpHeap(String dumpName) {
    try {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      String hotSpotDiagName = "com.sun.management:type=HotSpotDiagnostic";
      ObjectName name = new ObjectName(hotSpotDiagName);
      String operationName = "dumpHeap";

      new File("heapDumps").mkdirs();
      File tempFile = new File("heapDumps/" + dumpName + "_" + (System.currentTimeMillis()) + ".hprof");
      tempFile.delete();
      String dumpFilename = tempFile.getAbsolutePath();

      Object[] params = new Object[] { dumpFilename, Boolean.TRUE };
      String[] signature = new String[] { String.class.getName(), boolean.class.getName() };
      mbs.invoke(name, operationName, params, signature);

      System.out.println("dumped heap in file " + dumpFilename);
    } catch (Exception e) {
      // ignore
    }
  }

  private static class ThreadIgnore {
    private final String firstFramePackage;
    private final String threadNamePrefix;

    public ThreadIgnore(String threadNamePrefix) {
      this(threadNamePrefix, null);
    }

    public ThreadIgnore(String threadNamePrefix, String firstFramePackage) {
      this.threadNamePrefix = threadNamePrefix;
      this.firstFramePackage = firstFramePackage;
    }

    public boolean canIgnore(ThreadInfo info) {
      if (info.getThreadName().startsWith(threadNamePrefix)) {

        if (firstFramePackage == null) {
          return true;
        } else {
          StackTraceElement[] stack = info.getStackTrace();
          if (stack.length > 1) {
            StackTraceElement frame = stack[stack.length - 2];
            if (frame.getClassName().startsWith(firstFramePackage)) {
              return true;
            }
          } else {
            throw new AssertionError("Failed checking ignore for: " + info);
          }
        }
      }

      return false;
    }
  }
}

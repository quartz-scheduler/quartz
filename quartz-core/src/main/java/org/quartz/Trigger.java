
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

package org.quartz;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;



/**
 * 基本的接口伴随公共的属性对于所有的触发器
 * The base interface with properties common to all <code>Trigger</code>s -
 * 使用触发器构造者来实例化一个真真的触发器
 * use {@link TriggerBuilder} to instantiate an actual Trigger.
 * 
 * <p>
 *     触发器有一个触发器key,绑定到他们身上
 * <code>Triggers</code>s have a {@link TriggerKey} associated with them, which
 * 应该唯一识别他们使用一个单独的调度器
 * should uniquely identify them within a single <code>{@link Scheduler}</code>.
 * </p>
 * 
 * <p>
 *  触发器是一个机制来使作业呗调度
 * <code>Trigger</code>s are the 'mechanism' by which <code>Job</code>s
 * 许多触发器可以指向同一个job,但是一个单独的触发器可以只指向一个job
 * are scheduled. Many <code>Trigger</code>s can point to the same <code>Job</code>,
 * but a single <code>Trigger</code> can only point to one <code>Job</code>.
 * </p>
 * 
 * <p>
 *     触发器可以发送参数和数据到一个job,通过放置内容到jobDAtaMap在触发器
 * Triggers can 'send' parameters/data to <code>Job</code>s by placing contents
 * into the <code>JobDataMap</code> on the <code>Trigger</code>.
 * </p>
 *
 * @see TriggerBuilder
 * @see JobDataMap
 * @see JobExecutionContext
 * @see TriggerUtils
 * @see SimpleTrigger
 * @see CronTrigger
 * @see CalendarIntervalTrigger
 * 
 * @author James House
 */
public interface Trigger extends Serializable, Cloneable, Comparable<Trigger> {
    /**
     * 序列化默认
     */
    public static final long serialVersionUID = -3904243490805975570L;

    /**
     * 触发器状态枚举,没有,正常,暂停,完成,出错,阻塞
     */
    public enum TriggerState { NONE, NORMAL, PAUSED, COMPLETE, ERROR, BLOCKED }
    
    /**
     * noop还行调度器,trigger没有进一步的指令
     * <p><code>NOOP</code> Instructs the <code>{@link Scheduler}</code> that the 
     * <code>{@link Trigger}</code> has no further instructions.</p>
     * 可以重新执行的 job,指令 调度器,触发器想要jobdetail被立即重复执行.如果没有在恢复或者失败重试的情形
     * 执行的上下文将会被重新使用.是job有能力看到它上次执行的在上下文的任务
     * <p><code>RE_EXECUTE_JOB</code> Instructs the <code>{@link Scheduler}</code> that the 
     * <code>{@link Trigger}</code> wants the <code>{@link org.quartz.JobDetail}</code> to 
     * re-execute immediately. If not in a 'RECOVERING' or 'FAILED_OVER' situation, the
     * execution context will be re-used (giving the <code>Job</code> the
     * ability to 'see' anything placed in the context by its last execution).</p>
     * 设置触发器完成,指示调度器 触发器应该被放到完成状态
     * <p><code>SET_TRIGGER_COMPLETE</code> Instructs the <code>{@link Scheduler}</code> that the 
     * <code>{@link Trigger}</code> should be put in the <code>COMPLETE</code> state.</p>
     * 删除触发器,指示调度器,触发器想要删除他自己
     * <p><code>DELETE_TRIGGER</code> Instructs the <code>{@link Scheduler}</code> that the 
     * <code>{@link Trigger}</code> wants itself deleted.</p>
     * 设置所有job触发器的完成.指示触发器,所有引用同一个jobdetail的触发器应该被放到完成状态
     * <p><code>SET_ALL_JOB_TRIGGERS_COMPLETE</code> Instructs the <code>{@link Scheduler}</code> 
     * that all <code>Trigger</code>s referencing the same <code>{@link org.quartz.JobDetail}</code> 
     * as this one should be put in the <code>COMPLETE</code> state.</p>
     * 设置触发器错误,指示调度器,所有的触发器应该被设置到错误状态
     * <p><code>SET_TRIGGER_ERROR</code> Instructs the <code>{@link Scheduler}</code> that all 
     * <code>Trigger</code>s referencing the same <code>{@link org.quartz.JobDetail}</code> as
     * this one should be put in the <code>ERROR</code> state.</p>
     * 设置所有触发器状体错误,指示调度器,触发器应该被设置到错误状态
     * <p><code>SET_ALL_JOB_TRIGGERS_ERROR</code> Instructs the <code>{@link Scheduler}</code> that 
     * the <code>Trigger</code> should be put in the <code>ERROR</code> state.</p>
     */
    public enum CompletedExecutionInstruction { NOOP, RE_EXECUTE_JOB, SET_TRIGGER_COMPLETE, DELETE_TRIGGER, 
        SET_ALL_JOB_TRIGGERS_COMPLETE, SET_TRIGGER_ERROR, SET_ALL_JOB_TRIGGERS_ERROR }

    /**
     * 指示调度器 在一个失效的情形,在失效滞后方法将会被调用来决定失效的指令
     * 具体的逻辑将会被触发器的实现
     * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire
     * situation, the <code>updateAfterMisfire()</code> method will be called
     * on the <code>Trigger</code> to determine the mis-fire instruction,
     * which logic will be trigger-implementation-dependent.
     * 为了来观察这个指令是否满足你的需要,你应该寻找获取明智的失效策略方法来实现你正在使用的触发器
     * <p>
     * In order to see if this instruction fits your needs, you should look at
     * the documentation for the <code>getSmartMisfirePolicy()</code> method
     * on the particular <code>Trigger</code> implementation you are using.
     * </p>
     */
    public static final int MISFIRE_INSTRUCTION_SMART_POLICY = 0;
    
    /**
     * 指示调度器,触发器将会从不被评估,为了一个失效的情形
     * Instructs the <code>{@link Scheduler}</code> that the 
     * <code>Trigger</code> will never be evaluated for a misfire situation,
     * 调度器将会简单的试着去重新点火,然后更新触发器,就像他已经被在一个正确的时间调用
     * and that the scheduler will simply try to fire it as soon as it can, 
     * and then update the Trigger as if it had fired at the proper time. 
     * 如果一个触发器使用这个指令,他将丢失很好
     * <p>NOTE: if a trigger uses this instruction, and it has missed 
     * several of its scheduled firings, then several rapid firings may occur 
     * as the trigger attempt to catch back up to where it would have been. 
     * For example, a SimpleTrigger that fires every 15 seconds which has 
     * misfired for 5 minutes will fire 20 times once it gets the chance to 
     * fire.</p>
     */
    public static final int MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY = -1;
    
    /**
     * The default value for priority. 默认的优先级
     */
    public static final int DEFAULT_PRIORITY = 5;

    /**
     * 获取触发Key
     * @return
     */
    public TriggerKey getKey();

    /**
     * 获取jobKey
     * @return
     */
    public JobKey getJobKey();
    
    /**
     * Return the description given to the <code>Trigger</code> instance by
     * its creator (if any).
     * 返回触发器的描述,如果被创建者实例化
     * @return null if no description was set.
     */
    public String getDescription();

    /**
     * Get the name of the <code>{@link Calendar}</code> associated with this
     * Trigger.
     * 获取绑定到这个触发器的日历的名称
     * @return <code>null</code> if there is no associated Calendar.
     */
    public String getCalendarName();

    /**
     * Get the <code>JobDataMap</code> that is associated with the 
     * <code>Trigger</code>.
     * 获取jobDataMap绑定到这个触发器
     * <p>
     *     job执行的时候对这个map的变更将不能被持久化
     * Changes made to this map during job execution are not re-persisted, and
     * 实际上典型的结果是一个非法状态的异常
     * in fact typically result in an <code>IllegalStateException</code>.
     * </p>
     */
    public JobDataMap getJobDataMap();

    /**
     * The priority of a <code>Trigger</code> acts as a tiebreaker such that if 
     * two <code>Trigger</code>s have the same scheduled fire time, then the
     * one with the higher priority will get first access to a worker
     * thread.
     * 触发器的优先级,如果两个触发器有相同的触发实际,有高优先级的将会首先获取一个工作线程
     * <p>
     * If not explicitly set, the default value is <code>5</code>.
     * </p>
     * 
     * @see #DEFAULT_PRIORITY
     */
    public int getPriority();

    /**
     * Used by the <code>{@link Scheduler}</code> to determine whether or not
     * it is possible for this <code>Trigger</code> to fire again.
     * 使用调度器来决定是否他能够再次被触发
     * <p>
     *     返回值是false,调度器可能被移除触发器从jobstore
     * If the returned value is <code>false</code> then the <code>Scheduler</code>
     * may remove the <code>Trigger</code> from the <code>{@link org.quartz.spi.JobStore}</code>.
     * </p>
     */
    public boolean mayFireAgain();

    /**
     * Get the time at which the <code>Trigger</code> should occur.
     * 获取触发器应该发生的时间
     */
    public Date getStartTime();

    /**
     * 获取触发器应该退出重复的时间
     * Get the time at which the <code>Trigger</code> should quit repeating -
     * regardless of any remaining repeats (based on the trigger's particular 
     * repeat settings). 
     * 
     * @see #getFinalFireTime()
     */
    public Date getEndTime();

    /**
     * 返回下次触发掉被调度点火的时间,如果触发器将不会再被点火,null将会返回,注意时间
     * 返回可能是过去的,如果时间呗计算已经到达,但是调度器没有能够触发
     * Returns the next time at which the <code>Trigger</code> is scheduled to fire. If
     * the trigger will not fire again, <code>null</code> will be returned.  Note that
     * the time returned can possibly be in the past, if the time that was computed
     * for the trigger to next fire has already arrived, but the scheduler has not yet
     * been able to fire the trigger (which would likely be due to lack of resources
     * e.g. threads).
     *
     * <p>The value returned is not guaranteed to be valid until after the <code>Trigger</code>
     * has been added to the scheduler.
     * </p>
     *
     * @see TriggerUtils#computeFireTimesBetween(org.quartz.spi.OperableTrigger, Calendar, java.util.Date, java.util.Date)
     */
    public Date getNextFireTime();

    /**
     * 返回前一次触发器触发的时间,如果触发器没有被触发,null将会被返回
     * Returns the previous time at which the <code>Trigger</code> fired.
     * If the trigger has not yet fired, <code>null</code> will be returned.
     */
    public Date getPreviousFireTime();

    /**
     * 返回下次触发器被触发的时间在给定时时间之后,如果触发器将不会触发,null将会被返回
     * Returns the next time at which the <code>Trigger</code> will fire,
     * after the given time. If the trigger will not fire after the given time,
     * <code>null</code> will be returned.
     */
    public Date getFireTimeAfter(Date afterTime);

    /**
     * 返回触发器最近一次处理时间
     * Returns the last time at which the <code>Trigger</code> will fire, if
     * the Trigger will repeat indefinitely, null will be returned.
     * 触发器无限制重复,null将会返回
     * <p>
     * Note that the return time *may* be in the past.
     * </p>
     */
    public Date getFinalFireTime();

    /**
     * 获取调度器的指令来处理哑火的情形,
     * Get the instruction the <code>Scheduler</code> should be given for
     * handling misfire situations for this <code>Trigger</code>- the
     * concrete <code>Trigger</code> type that you are using will have
     * defined a set of additional <code>MISFIRE_INSTRUCTION_XXX</code>
     * constants that may be set as this property's value.
     * 
     * <p>
     * If not explicitly set, the default value is <code>MISFIRE_INSTRUCTION_SMART_POLICY</code>.
     * </p>
     * 
     * @see #MISFIRE_INSTRUCTION_SMART_POLICY
     * @see SimpleTrigger
     * @see CronTrigger
     */
    public int getMisfireInstruction();

    /**
     * 获取一个triggerBuilder被配置来处理一个触发器
     * Get a {@link TriggerBuilder} that is configured to produce a 
     * <code>Trigger</code> identical to this one.
     * 
     * @see #getScheduleBuilder()
     */
    public TriggerBuilder<? extends Trigger> getTriggerBuilder();
    
    /**
     * 获取一个调度器来生成一个调度示例
     * Get a {@link ScheduleBuilder} that is configured to produce a 
     * schedule identical to this trigger's schedule.
     * 
     * @see #getTriggerBuilder()
     */
    public ScheduleBuilder<? extends Trigger> getScheduleBuilder();

    /**
     * Trigger equality is based upon the equality of the TriggerKey.
     * trigger相等基于triggerKey的相等
     * @return true if the key of this Trigger equals that of the given Trigger.
     */
    public boolean equals(Object other);
    
    /**
     * <p>
     * Compare the next fire time of this <code>Trigger</code> to that of
     * another by comparing their keys, or in other words, sorts them
     * according to the natural (i.e. alphabetical) order of their keys.
     * </p>
     */
    public int compareTo(Trigger other);

    /**
     * A Comparator that compares trigger's next fire times, or in other words,
     * sorts them according to earliest next fire time.  If the fire times are
     * the same, then the triggers are sorted according to priority (highest
     * value first), if the priorities are the same, then they are sorted
     * by key.
     */
    class TriggerTimeComparator implements Comparator<Trigger>, Serializable {
      
        private static final long serialVersionUID = -3904243490805975570L;
        
        // This static method exists for comparator in TC clustered quartz
        public static int compare(Date nextFireTime1, int priority1, TriggerKey key1, Date nextFireTime2, int priority2, TriggerKey key2) {
            if (nextFireTime1 != null || nextFireTime2 != null) {
                if (nextFireTime1 == null) {
                    return 1;
                }

                if (nextFireTime2 == null) {
                    return -1;
                }

                if(nextFireTime1.before(nextFireTime2)) {
                    return -1;
                }

                if(nextFireTime1.after(nextFireTime2)) {
                    return 1;
                }
            }

            int comp = priority2 - priority1;
            if (comp != 0) {
                return comp;
            }

            return key1.compareTo(key2);
        }


        public int compare(Trigger t1, Trigger t2) {
            return compare(t1.getNextFireTime(), t1.getPriority(), t1.getKey(), t2.getNextFireTime(), t2.getPriority(), t2.getKey());
        }
    }
}

package org.quartz.impl.calendar;

import java.text.ParseException;
import java.util.Date;

import org.quartz.CronExpression;

/**
 * This implementation of the Calendar excludes the set of times expressed by a
 * given {@link org.quartz.CronExpression CronExpression}. For example, you 
 * could use this calendar to exclude all but business hours (8AM - 5PM) every 
 * day using the expression &quot;* * 0-7,18-24 ? * *&quot;. 
 * <P>
 * It is important to remember that the cron expression here describes a set of
 * times to be <I>excluded</I> from firing. Whereas the cron expression in 
 * {@link org.quartz.CronTrigger CronTrigger} describes a set of times that can
 * be <I>included</I> for firing. Thus, if a <CODE>CronTrigger</CODE> has a 
 * given cron expression and is associated with a <CODE>CronCalendar</CODE> with
 * the <I>same</I> expression, the calendar will exclude all the times the 
 * trigger includes, and they will cancel each other out. 
 * 
 * @author Aaron Craven
 * @version $Revision$ $Date$
 */
public class CronCalendar extends BaseCalendar {
    private String name;
    CronExpression cronExpression;

    /**
     * Create a <CODE>CronCalendar</CODE> with the given cron expression and no
     * <CODE>baseCalendar</CODE>.
     *  
     * @param name       the name for the <CODE>DailyCalendar</CODE>
     * @param expression a String representation of the desired cron expression
     */
    public CronCalendar(String name, String expression) 
    		throws ParseException {
        super();
        this.name = name;
        this.cronExpression = new CronExpression(expression);
    }

    /**
     * Create a <CODE>CronCalendar</CODE> with the given cron exprssion and 
     * <CODE>baseCalendar</CODE>. 
     * 
     * @param name         the name for the <CODE>DailyCalendar</CODE>
     * @param baseCalendar the base calendar for this calendar instance &ndash;
     *                     see {@link BaseCalendar} for more information on base
     *                     calendar functionality
     * @param expression   a String representation of the desired cron expression
     */
    public CronCalendar(String name, org.quartz.Calendar baseCalendar,
    		String expression) throws ParseException {
        super(baseCalendar);
        this.name = name;
        this.cronExpression = new CronExpression(expression);
    }

    /**
     * Returns the name of the <CODE>CronCalendar</CODE>
     * 
     * @return the name of the <CODE>CronCalendar</CODE>
     */
    public String getName() {
        return name;
    }

    /**
     * Determines whether the given time (in milliseconds) is 'included' by the
     * <CODE>BaseCalendar</CODE>
     * 
     * @param timeInMillis the date/time to test
     * @return a boolean indicating whether the specified time is 'included' by
     *         the <CODE>CronCalendar</CODE>
     */
    public boolean isTimeIncluded(long timeInMillis) {        
        if ((getBaseCalendar() != null) && 
        		(getBaseCalendar().isTimeIncluded(timeInMillis) == false)) {
        	return false;
        }
        
        return (!(cronExpression.isSatisfiedBy(new Date(timeInMillis))));
    }

    /**
     * Determines the next time included by the <CODE>CronCalendar</CODE>
     * after the specified time.
     * 
     * @param timeInMillis the initial date/time after which to find an 
     *                     included time
     * @return the time in milliseconds representing the next time included
     *         after the specified time.
     */
    public long getNextIncludedTime(long timeInMillis) {
        long nextIncludedTime = timeInMillis + 1; //plus on millisecond
        
        while (!isTimeIncluded(nextIncludedTime)) {
        	//If the time is in a range excluded by this calendar, we can
        	// move to the end of the excluded time range and continue testing
        	// from there. Otherwise, if nextIncludedTime is excluded by the
        	// baseCalendar, ask it the next time it includes and begin testing
        	// from there. Failing this, add one millisecond and continue
        	// testing.
        	if (cronExpression.isSatisfiedBy(new Date(nextIncludedTime))) {
        		nextIncludedTime = 
        			cronExpression.getNextValidTimeAfter(
        					new Date(nextIncludedTime)).getTime();
        	} else if ((getBaseCalendar() != null) && 
        			(!getBaseCalendar().isTimeIncluded(nextIncludedTime))){
        		nextIncludedTime = 
        			getBaseCalendar().getNextIncludedTime(nextIncludedTime);
        	} else {
        		nextIncludedTime++;
        	}
        }
        
        return nextIncludedTime;
    }

    /**
     * Returns a string representing the properties of the 
     * <CODE>CronCalendar</CODE>
     * 
     * @return the properteis of the CronCalendar in a String format
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(getName());
        buffer.append(": base calendar: [");
        if (getBaseCalendar() != null) {
        	buffer.append(getBaseCalendar().toString());
        } else {
        	buffer.append("null");
        }
        buffer.append("], excluded cron expression: '");
        buffer.append(cronExpression);
        buffer.append("'");
        return buffer.toString();
    }
    
    /**
     * Returns the object representation of the cron expression that defines the
     * dates and times this calendar excludes.
     * 
     * @return the cron expression
     * @see org.quartz.CronExpression
     */
    public CronExpression getCronExpression() {
    	return cronExpression;
    }
    
    /**
     * Sets the cron expression for the calendar to a new value
     * 
     * @param expression the new string value to build a cron expression from
     * @throws ParseException
     *         if the string expression cannot be parsed
     */
    public void setCronExpression(String expression) throws ParseException {
    	CronExpression newExp = new CronExpression(expression);
    	
    	this.cronExpression = newExp;
    }

    /**
     * Sets the cron expression for the calendar to a new value
     * 
     * @param expression the new cron expression
     */
    public void setCronExpression(CronExpression expression) {
    	if (expression == null) {
    		throw new IllegalArgumentException("expression cannot be null");
    	}
    	
    	this.cronExpression = expression;
    }
}
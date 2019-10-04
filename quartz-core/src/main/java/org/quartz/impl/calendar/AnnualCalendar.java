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

package org.quartz.impl.calendar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TimeZone;

import org.quartz.Calendar;

/**
 * <p>
 * This implementation of the Calendar excludes a set of days of the year. You
 * may use it to exclude bank holidays which are on the same date every year.
 * </p>
 * 
 * @see org.quartz.Calendar
 * @see org.quartz.impl.calendar.BaseCalendar
 * 
 * @author Juergen Donnerstag
 */
public class AnnualCalendar extends BaseCalendar implements Calendar,
        Serializable {

    static final long serialVersionUID = 7346867105876610961L;

    private ArrayList<java.util.Calendar> excludeDays = new ArrayList<java.util.Calendar>();

    // true, if excludeDays is sorted
    private boolean dataSorted = false;

    public AnnualCalendar() {
    }

    public AnnualCalendar(Calendar baseCalendar) {
        super(baseCalendar);
    }

    public AnnualCalendar(TimeZone timeZone) {
        super(timeZone);
    }

    public AnnualCalendar(Calendar baseCalendar, TimeZone timeZone) {
        super(baseCalendar, timeZone);
    }

    @Override
    public Object clone() {
        AnnualCalendar clone = (AnnualCalendar) super.clone();
        clone.excludeDays = new ArrayList<java.util.Calendar>(excludeDays);
        return clone;
    }

    /**
     * <p>
     * Get the array which defines the exclude-value of each day of month
     * </p>
     */
    public ArrayList<java.util.Calendar> getDaysExcluded() {
        return excludeDays;
    }

    /**
     * <p>
     * Return true, if day is defined to be exluded.
     * </p>
     */
    public boolean isDayExcluded(java.util.Calendar day) {

        if (day == null) {
            throw new IllegalArgumentException(
                    "Parameter day must not be null");
        }

         // Check baseCalendar first
        if (! super.isTimeIncluded(day.getTime().getTime())) {
         return true;
        } 
        
        int dmonth = day.get(java.util.Calendar.MONTH);
        int dday = day.get(java.util.Calendar.DAY_OF_MONTH);

        if (dataSorted == false) {
            Collections.sort(excludeDays, new CalendarComparator());
            dataSorted = true;
        }

        Iterator<java.util.Calendar> iter = excludeDays.iterator();
        while (iter.hasNext()) {
            java.util.Calendar cl = (java.util.Calendar) iter.next();

            // remember, the list is sorted
            if (dmonth < cl.get(java.util.Calendar.MONTH)) {
                return false;
            }

            if (dday != cl.get(java.util.Calendar.DAY_OF_MONTH)) {
                continue;
            }

            if (dmonth != cl.get(java.util.Calendar.MONTH)) {
                continue;
            }

            return true;
        }

        return false;
    }

    /**
     * <p>
     * Redefine the list of days excluded. The ArrayList 
     * should contain <code>java.util.Calendar</code> objects. 
     * </p>
     */
    public void setDaysExcluded(ArrayList<java.util.Calendar> days) {
        if (days == null) {
            excludeDays = new ArrayList<java.util.Calendar>();
        } else {
            excludeDays = days;
        }

        dataSorted = false;
    }

    /**
     * <p>
     * Redefine a certain day to be excluded (true) or included (false).
     * </p>
     */
    public void setDayExcluded(java.util.Calendar day, boolean exclude) {
        if (exclude) {
            if (isDayExcluded(day)) {
                return;
            }

            excludeDays.add(day);
            dataSorted = false;
        } else {
            if (!isDayExcluded(day)) {
                return;
            }

            removeExcludedDay(day, true);
        }
    }

    /**
     * Remove the given day from the list of excluded days
     *  
     * @param day the day to exclude
     */
    public void removeExcludedDay(java.util.Calendar day) {
        removeExcludedDay(day, false);
    }
    
    private void removeExcludedDay(java.util.Calendar day, boolean isChecked) {
        if (! isChecked &&
            ! isDayExcluded(day)) {
            return;
        }
        
        // Fast way, see if exact day object was already in list
        if (this.excludeDays.remove(day)) {
            return;
        }
        
        int dmonth = day.get(java.util.Calendar.MONTH);
        int dday = day.get(java.util.Calendar.DAY_OF_MONTH);
        
        // Since there is no guarantee that the given day is in the arraylist with the exact same year
        // search for the object based on month and day of month in the list and remove it
        Iterator<java.util.Calendar> iter = excludeDays.iterator();
        while (iter.hasNext()) {
            java.util.Calendar cl = (java.util.Calendar) iter.next();

            if (dmonth != cl.get(java.util.Calendar.MONTH)) {
                continue;
            }

            if (dday != cl.get(java.util.Calendar.DAY_OF_MONTH)) {
                continue;
            }

            day = cl;
            break;
        }
        
        this.excludeDays.remove(day);
    }

    
    /**
     * <p>
     * Determine whether the given time (in milliseconds) is 'included' by the
     * Calendar.
     * </p>
     * 
     * <p>
     * Note that this Calendar is only has full-day precision.
     * </p>
     */
    @Override
    public boolean isTimeIncluded(long timeStamp) {
        // Test the base calendar first. Only if the base calendar not already
        // excludes the time/date, continue evaluating this calendar instance.
        if (super.isTimeIncluded(timeStamp) == false) { return false; }

        java.util.Calendar day = createJavaCalendar(timeStamp);

        return !(isDayExcluded(day));
    }

    /**
     * <p>
     * Determine the next time (in milliseconds) that is 'included' by the
     * Calendar after the given time. Return the original value if timeStamp is
     * included. Return 0 if all days are excluded.
     * </p>
     * 
     * <p>
     * Note that this Calendar is only has full-day precision.
     * </p>
     */
    @Override
    public long getNextIncludedTime(long timeStamp) {
        // Call base calendar implementation first
        long baseTime = super.getNextIncludedTime(timeStamp);
        if ((baseTime > 0) && (baseTime > timeStamp)) {
            timeStamp = baseTime;
        }

        // Get timestamp for 00:00:00
        java.util.Calendar day = getStartOfDayJavaCalendar(timeStamp);
        if (isDayExcluded(day) == false) { 
            return timeStamp; // return the original value
        }

        while (isDayExcluded(day) == true) {
            day.add(java.util.Calendar.DATE, 1);
        }

        return day.getTime().getTime();
    }
}

class CalendarComparator implements Comparator<java.util.Calendar>, Serializable {
  
    private static final long serialVersionUID = 7346867105876610961L;
    
    public CalendarComparator() {
    }


    public int compare(java.util.Calendar c1, java.util.Calendar c2) {
        
        int month1 = c1.get(java.util.Calendar.MONTH);
        int month2 = c2.get(java.util.Calendar.MONTH);
        
        int day1 = c1.get(java.util.Calendar.DAY_OF_MONTH);
        int day2 = c2.get(java.util.Calendar.DAY_OF_MONTH);
        
        if (month1 < month2) {
            return -1;
        }
        if (month1 > month2) {
            return 1; 
        }
        if (day1 < day2) {
            return -1;
        }
        if (day1 > day2) {
            return 1;
        }
        return 0;
      }
}

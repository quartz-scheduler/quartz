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
package org.quartz.simpl;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.spi.TriggerFiredBundle;



/**
 * A JobFactory that instantiates the Job instance (using the default no-arg
 * constructor, or more specifically: <code>class.newInstance()</code>), and
 * then attempts to set all values from the <code>SchedulerContext</code> and
 * the <code>JobExecutionContext</code>'s merged <code>JobDataMap</code> onto 
 * bean properties of the <code>Job</code>.
 * 
 * <p>Set the warnIfPropertyNotFound property to true if you'd like noisy logging in
 * the case of values in the JobDataMap not mapping to properties on your Job
 * class.  This may be useful for troubleshooting typos of property names, etc.
 * but very noisy if you regularly (and purposely) have extra things in your
 * JobDataMap.</p>
 * 
 * <p>Also of possible interest is the throwIfPropertyNotFound property which
 * will throw exceptions on unmatched JobDataMap keys.</p>
 * 
 * @see org.quartz.spi.JobFactory
 * @see SimpleJobFactory
 * @see SchedulerContext
 * @see org.quartz.JobExecutionContext#getMergedJobDataMap()
 * @see #setWarnIfPropertyNotFound(boolean)
 * @see #setThrowIfPropertyNotFound(boolean)
 * 
 * @author jhouse
 */
public class PropertySettingJobFactory extends SimpleJobFactory {
    private boolean warnIfNotFound = false;
    private boolean throwIfNotFound = false;
    
    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {

        Job job = super.newJob(bundle, scheduler);
        
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.putAll(scheduler.getContext());
        jobDataMap.putAll(bundle.getJobDetail().getJobDataMap());
        jobDataMap.putAll(bundle.getTrigger().getJobDataMap());

        setBeanProps(job, jobDataMap);
        
        return job;
    }
    
    protected void setBeanProps(Object obj, JobDataMap data) throws SchedulerException {

        BeanInfo bi = null;
        try {
            bi = Introspector.getBeanInfo(obj.getClass());
        } catch (IntrospectionException e) {
            handleError("Unable to introspect Job class.", e);
        }
        
        PropertyDescriptor[] propDescs = bi.getPropertyDescriptors();
        
        // Get the wrapped entry set so don't have to incur overhead of wrapping for
        // dirty flag checking since this is read only access
        for (Iterator<?> entryIter = data.getWrappedMap().entrySet().iterator(); entryIter.hasNext();) {
            Map.Entry<?,?> entry = (Map.Entry<?,?>)entryIter.next();
            
            String name = (String)entry.getKey();
            String c = name.substring(0, 1).toUpperCase(Locale.US);
            String methName = "set" + c + name.substring(1);
        
            java.lang.reflect.Method setMeth = getSetMethod(methName, propDescs);
        
            Class<?> paramType = null;
            Object o = null;
            
            try {
                if (setMeth == null) {
                    handleError(
                        "No setter on Job class " + obj.getClass().getName() + 
                        " for property '" + name + "'");
                    continue;
                }
                
                paramType = setMeth.getParameterTypes()[0];
                o = entry.getValue();
                
                Object parm = null;
                if (paramType.isPrimitive()) {
                    if (o == null) {
                        handleError(
                            "Cannot set primitive property '" + name + 
                            "' on Job class " + obj.getClass().getName() + 
                            " to null.");
                        continue;
                    }

                    if (paramType.equals(int.class)) {
                        if (o instanceof String) {                            
                            parm = Integer.valueOf((String)o);
                        } else if (o instanceof Integer) {
                            parm = o;
                        }
                    } else if (paramType.equals(long.class)) {
                        if (o instanceof String) {
                            parm = Long.valueOf((String)o);
                        } else if (o instanceof Long) {
                            parm = o;
                        }
                    } else if (paramType.equals(float.class)) {
                        if (o instanceof String) {
                            parm = Float.valueOf((String)o);
                        } else if (o instanceof Float) {
                            parm = o;
                        }
                    } else if (paramType.equals(double.class)) {
                        if (o instanceof String) {
                            parm = Double.valueOf((String)o);
                        } else if (o instanceof Double) {
                            parm = o;
                        }
                    } else if (paramType.equals(boolean.class)) {
                        if (o instanceof String) {
                            parm = Boolean.valueOf((String)o);
                        } else if (o instanceof Boolean) {
                            parm = o;
                        }
                    } else if (paramType.equals(byte.class)) {
                        if (o instanceof String) {
                            parm = Byte.valueOf((String)o);
                        } else if (o instanceof Byte) {
                            parm = o;
                        }
                    } else if (paramType.equals(short.class)) {
                        if (o instanceof String) {
                            parm = Short.valueOf((String)o);
                        } else if (o instanceof Short) {
                            parm = o;
                        }
                    } else if (paramType.equals(char.class)) {
                        if (o instanceof String) {
                            String str = (String)o;
                            if (str.length() == 1) {
                                parm = Character.valueOf(str.charAt(0));
                            }
                        } else if (o instanceof Character) {
                            parm = o;
                        }
                    }
                } else if ((o != null) && (paramType.isAssignableFrom(o.getClass()))) {
                    parm = o;
                }
                
                // If the parameter wasn't originally null, but we didn't find a 
                // matching parameter, then we are stuck.
                if ((o != null) && (parm == null)) {
                    handleError(
                        "The setter on Job class " + obj.getClass().getName() + 
                        " for property '" + name + 
                        "' expects a " + paramType + 
                        " but was given " + o.getClass().getName());
                    continue;
                }
                                
                setMeth.invoke(obj, new Object[]{ parm });
            } catch (NumberFormatException nfe) {
                handleError(
                    "The setter on Job class " + obj.getClass().getName() + 
                    " for property '" + name + 
                    "' expects a " + paramType + 
                    " but was given " + o.getClass().getName(), nfe);
            } catch (IllegalArgumentException e) {
                handleError(
                    "The setter on Job class " + obj.getClass().getName() + 
                    " for property '" + name + 
                    "' expects a " + paramType + 
                    " but was given " + o.getClass().getName(), e);
            } catch (IllegalAccessException e) {
                handleError(
                    "The setter on Job class " + obj.getClass().getName() + 
                    " for property '" + name + 
                    "' could not be accessed.", e);
            } catch (InvocationTargetException e) {
                handleError(
                    "The setter on Job class " + obj.getClass().getName() + 
                    " for property '" + name + 
                    "' could not be invoked.", e);
            }
        }
    }
     
    private void handleError(String message) throws SchedulerException {
        handleError(message, null);
    }
    
    private void handleError(String message, Exception e) throws SchedulerException {
        if (isThrowIfPropertyNotFound()) {
            throw new SchedulerException(message, e);
        }
        
        if (isWarnIfPropertyNotFound()) {
            if (e == null) {
                getLog().warn(message);
            } else {
                getLog().warn(message, e);
            }
        }
    }
    
    private java.lang.reflect.Method getSetMethod(String name,
            PropertyDescriptor[] props) {
        for (int i = 0; i < props.length; i++) {
            java.lang.reflect.Method wMeth = props[i].getWriteMethod();
        
            if(wMeth == null) {
                continue;
            }
            
            if(wMeth.getParameterTypes().length != 1) {
                continue;
            }
            
            if (wMeth.getName().equals(name)) {
                return wMeth;
            }
        }
        
        return null;
    }

    /**
     * Whether the JobInstantiation should fail and throw and exception if
     * a key (name) and value (type) found in the JobDataMap does not 
     * correspond to a proptery setter on the Job class.
     *  
     * @return Returns the throwIfNotFound.
     */
    public boolean isThrowIfPropertyNotFound() {
        return throwIfNotFound;
    }

    /**
     * Whether the JobInstantiation should fail and throw and exception if
     * a key (name) and value (type) found in the JobDataMap does not 
     * correspond to a proptery setter on the Job class.
     *  
     * @param throwIfNotFound defaults to <code>false</code>.
     */
    public void setThrowIfPropertyNotFound(boolean throwIfNotFound) {
        this.throwIfNotFound = throwIfNotFound;
    }

    /**
     * Whether a warning should be logged if
     * a key (name) and value (type) found in the JobDataMap does not 
     * correspond to a proptery setter on the Job class.
     *  
     * @return Returns the warnIfNotFound.
     */
    public boolean isWarnIfPropertyNotFound() {
        return warnIfNotFound;
    }

    /**
     * Whether a warning should be logged if
     * a key (name) and value (type) found in the JobDataMap does not 
     * correspond to a proptery setter on the Job class.
     *  
     * @param warnIfNotFound defaults to <code>true</code>.
     */
    public void setWarnIfPropertyNotFound(boolean warnIfNotFound) {
        this.warnIfNotFound = warnIfNotFound;
    }
}
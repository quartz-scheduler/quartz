package org.quartz.simpl;

import org.quartz.SchedulerException;
import org.quartz.spi.InstanceIdGenerator;

/**
 * InstanceIdGenerator that will use a {@link SystemPropertyInstanceIdGenerator#SYSTEM_PROPERTY system property}
 * to configure the scheduler.  The default system property name to use the value of {@link #SYSTEM_PROPERTY}, but
 * can be specified via the "systemPropertyName" property.
 * 
 * You can also set the properties "postpend" and "prepend" to String values that will be added to the beginning
 * or end (respectively) of the value found in the system property.
 * 
 * If no value set for the property, a {@link org.quartz.SchedulerException} is thrown
 *
 * @author Alex Snaps
 */
public class SystemPropertyInstanceIdGenerator implements InstanceIdGenerator {

  /**
   * System property to read the instanceId from
   */
  public static final String SYSTEM_PROPERTY = "org.quartz.scheduler.instanceId";

  private String prepend = null;
  private String postpend = null;
  private String systemPropertyName = SYSTEM_PROPERTY;
  
  /**
   * Returns the cluster wide value for this scheduler instance's id, based on a system property
   * @return the value of the system property named by the value of {@link #getSystemPropertyName()} - which defaults
   * to {@link #SYSTEM_PROPERTY}.
   * @throws SchedulerException Shouldn't a value be found
   */
  public String generateInstanceId() throws SchedulerException {
    String property = System.getProperty(getSystemPropertyName());
    if(property == null) {
      throw new SchedulerException("No value for '" + SYSTEM_PROPERTY
                                   + "' system property found, please configure your environment accordingly!");
    }
    if(getPrepend() != null)
        property = getPrepend() + property;
    if(getPostpend() != null)
        property = property + getPostpend();
    
    return property;
  }
  
  /**
   * A String of text to prepend (add to the beginning) to the instanceId 
   * found in the system property.
   */
  public String getPrepend() {
    return prepend;
  }

  /**
   * A String of text to prepend (add to the beginning) to the instanceId 
   * found in the system property.
   * 
   * @param prepend the value to prepend, or null if none is desired.
   */
  public void setPrepend(String prepend) {
    this.prepend = prepend == null ?  null  : prepend.trim();
  }
    
  /**
   * A String of text to postpend (add to the end) to the instanceId 
   * found in the system property.
   */
  public String getPostpend() {
    return postpend;
  }

  /**
   * A String of text to postpend (add to the end) to the instanceId 
   * found in the system property.
   * 
   * @param postpend the value to postpend, or null if none is desired.
   */
  public void setPostpend(String postpend) {
    this.postpend = postpend == null ?  null : postpend.trim();
  }

  /**
   * The name of the system property from which to obtain the instanceId.
   * 
   * Defaults to {@link #SYSTEM_PROPERTY}.
   * 
   */  
  public String getSystemPropertyName() {
    return systemPropertyName;
  }

  /**
   * The name of the system property from which to obtain the instanceId.
   * 
   * Defaults to {@link #SYSTEM_PROPERTY}.
   * 
   * @param systemPropertyName the system property name
   */
  public void setSystemPropertyName(String systemPropertyName) {
    this.systemPropertyName = systemPropertyName == null ? SYSTEM_PROPERTY : systemPropertyName.trim();
  }
  
} 

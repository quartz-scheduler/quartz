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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.transaction.UserTransaction;

/**
 * An annotation that marks a {@link Job} class as one that will have its 
 * execution wrapped by a JTA Transaction. 
 *   
 * <p>If this annotation is present, Quartz will begin a JTA transaction 
 * before calling the <code>execute()</code> method, and will commit
 * the transaction if the method does not throw an exception and the
 * transaction has not had <code>setRollbackOnly()</code> called on it 
 * (otherwise the transaction will be rolled-back by Quartz).</p>
 * 
 * <p>This is essentially the same behavior as setting the configuration
 * property <code>org.quartz.scheduler.wrapJobExecutionInUserTransaction</code>
 * to <code>true</code> - except that it only affects the job that has
 * the annotation, rather than all jobs (as the property does).  If the
 * property is set to <code>true</code> and the annotation is also set,
 * then of course the annotation becomes redundant.</p> 
 * 
 * @author jhouse
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExecuteInJTATransaction {

  /**
   * The JTA transaction timeout.
   * <p>
   * If set then the {@code UserTransaction} timeout will be set to this
   * value before beginning the transaction.
   * 
   * @see UserTransaction#setTransactionTimeout(int) 
   * @return the transaction timeout.
   */
  int timeout() default -1;
}

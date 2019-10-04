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

import org.quartz.Scheduler;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

public class Client2 extends ClientBase {

  public static final CyclicBarrier localBarrier = new CyclicBarrier(2);

  public static void main(String[] args) {
    new Client2(args).run();
  }

  public Client2(String args[]) {
    super(args);
  }

  @Override
  protected void test(Scheduler scheduler) throws Throwable {
    // job should be recovered and trip barrier
    localBarrier.await(3, TimeUnit.MINUTES);
  }
}

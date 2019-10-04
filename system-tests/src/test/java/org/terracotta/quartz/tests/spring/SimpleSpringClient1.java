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
 package org.terracotta.quartz.tests.spring;

import org.quartz.Scheduler;

import java.util.concurrent.CyclicBarrier;

public class SimpleSpringClient1 extends SpringClientBase {

  public static final CyclicBarrier localBarrier = new CyclicBarrier(2);

  public SimpleSpringClient1(String[] args) {
    super("simple-spring-client1.xml", args);
  }

  public static void main(String[] args) {
    new SimpleSpringClient1(args).run();
  }

  @Override
  protected void test0(Scheduler scheduler) throws Throwable {
    localBarrier.await();
  }
}

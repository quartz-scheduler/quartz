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

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Date;

public class NodeDeathTestClient2 extends ClientBase {

  private final int node1Port;

  public NodeDeathTestClient2(String[] args) {
    super(args);
    this.node1Port = Integer.parseInt(System.getProperty("listenPort"));
  }

  @Override
  protected void test(Scheduler scheduler) throws Throwable {
    Socket s = null;

    // make this loop forever in case a hang in the first client prevents the listener from ever starting. This loop
    // should make the test timeout and thus we'll get a thread dump
    while (true) {
      try {
        s = new Socket("127.0.0.1", node1Port);
        break;
      } catch (ConnectException ce) {
        Thread.sleep(1000L);
      }
    }

    InputStream in = s.getInputStream();

    final long end = System.currentTimeMillis() + 60000L;
    while (System.currentTimeMillis() < end) {
      log("Sleeping");
      Thread.sleep(1000L);

      try {
        if (in.read() < 0) {
          break;
        }
      } catch (IOException ioe) {
        break;
      }
    }
  }

  private void log(String msg) {
    System.err.println("[" + new Date() + "] " + msg);
  }
}

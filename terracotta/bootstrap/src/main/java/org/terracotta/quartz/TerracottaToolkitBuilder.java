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

package org.terracotta.quartz;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.ToolkitInstantiationException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class TerracottaToolkitBuilder {

  private static final String      TC_TUNNELLED_MBEAN_DOMAIN_KEY = "tunnelledMBeanDomains";
  private static final String      TC_CONFIG_SNIPPET_KEY         =  "tcConfigSnippet";
  private static final String      TC_REJOIN_KEY                 = "rejoin";
  private final TCConfigTypeStatus tcConfigTypeStatus            = new TCConfigTypeStatus();
  private final Set<String>        tunnelledMBeanDomains         = Collections.synchronizedSet(new HashSet<String>());

  private boolean                  rejoin                        = false;
  
  public Toolkit buildToolkit() throws IllegalStateException {
    if (tcConfigTypeStatus.getState() == TCConfigTypeState.INIT) {
      //
      throw new IllegalStateException(
                                      "Please set the tcConfigSnippet or tcConfigUrl before attempting to create client");
    }
    Properties properties = new Properties();
    properties.setProperty(TC_TUNNELLED_MBEAN_DOMAIN_KEY, getTunnelledDomainCSV());
    properties.setProperty(TC_REJOIN_KEY, Boolean.toString(isRejoin()));
    switch (tcConfigTypeStatus.getState()) {
      case TC_CONFIG_SNIPPET:
        properties.setProperty(TC_CONFIG_SNIPPET_KEY, tcConfigTypeStatus.getTcConfigSnippet());
        return createToolkit("toolkit:terracotta:", properties);
      case TC_CONFIG_URL:
        return createToolkit("toolkit:terracotta://" + tcConfigTypeStatus.getTcConfigUrl(), properties);
      default:
        throw new IllegalStateException("Unknown tc config type - " + tcConfigTypeStatus.getState());
    }
  }

  private Toolkit createToolkit(String toolkitUrl, Properties props) {
    try {
      return ToolkitFactory.createToolkit(toolkitUrl, props);
    } catch (ToolkitInstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  private String getTunnelledDomainCSV() {
    StringBuilder sb = new StringBuilder();
    for (String domain : tunnelledMBeanDomains) {
      sb.append(domain).append(",");
    }
    // remove last comma
    return sb.deleteCharAt(sb.length() - 1).toString();
  }

  public TerracottaToolkitBuilder addTunnelledMBeanDomain(String tunnelledMBeanDomain) {
    this.tunnelledMBeanDomains.add(tunnelledMBeanDomain);
    return this;
  }

  public Set<String> getTunnelledMBeanDomains() {
    return Collections.unmodifiableSet(tunnelledMBeanDomains);
  }

  public TerracottaToolkitBuilder removeTunnelledMBeanDomain(String tunnelledMBeanDomain) {
    tunnelledMBeanDomains.remove(tunnelledMBeanDomain);
    return this;
  }

  public TerracottaToolkitBuilder setTCConfigSnippet(String tcConfig) throws IllegalStateException {
    tcConfigTypeStatus.setTcConfigSnippet(tcConfig);
    return this;
  }

  public String getTCConfigSnippet() {
    return tcConfigTypeStatus.getTcConfigSnippet();
  }

  public TerracottaToolkitBuilder setTCConfigUrl(String tcConfigUrl) throws IllegalStateException {
    tcConfigTypeStatus.setTcConfigUrl(tcConfigUrl);
    return this;
  }

  public String getTCConfigUrl() {
    return tcConfigTypeStatus.getTcConfigUrl();
  }

  public boolean isConfigUrl() {
    return tcConfigTypeStatus.getState() == TCConfigTypeState.TC_CONFIG_URL;
  }

  public TerracottaToolkitBuilder setRejoin(String rejoin) {
    this.rejoin = Boolean.valueOf(rejoin);
    return this;
  }

  public boolean isRejoin() {
    return rejoin;
  }

  private static enum TCConfigTypeState {
    INIT, TC_CONFIG_SNIPPET, TC_CONFIG_URL;
  }

  private static class TCConfigTypeStatus {
    private TCConfigTypeState state = TCConfigTypeState.INIT;

    private String            tcConfigSnippet;
    private String            tcConfigUrl;

    public synchronized void setTcConfigSnippet(String tcConfigSnippet) {
      if (state == TCConfigTypeState.TC_CONFIG_URL) {
        //
        throw new IllegalStateException("TCConfig url was already set to - " + tcConfigUrl);
      }
      this.state = TCConfigTypeState.TC_CONFIG_SNIPPET;
      this.tcConfigSnippet = tcConfigSnippet;
    }

    public synchronized void setTcConfigUrl(String tcConfigUrl) {
      if (state == TCConfigTypeState.TC_CONFIG_SNIPPET) {
        //
        throw new IllegalStateException("TCConfig snippet was already set to - " + tcConfigSnippet);
      }
      this.state = TCConfigTypeState.TC_CONFIG_URL;
      this.tcConfigUrl = tcConfigUrl;
    }

    public synchronized String getTcConfigSnippet() {
      return tcConfigSnippet;
    }

    public synchronized String getTcConfigUrl() {
      return tcConfigUrl;
    }

    public TCConfigTypeState getState() {
      return state;
    }

  }

}

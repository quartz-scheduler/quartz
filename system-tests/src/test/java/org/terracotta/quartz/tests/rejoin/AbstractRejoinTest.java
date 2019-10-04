/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.quartz.tests.rejoin;

import com.tc.test.config.model.TestConfig;

import java.util.Properties;

import org.terracotta.quartz.AbstractTerracottaJobStore;
import org.terracotta.quartz.tests.AbstractStandaloneTest;
import org.terracotta.quartz.tests.ClientBase;

import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.tests.base.AbstractClientBase;

/**
 *
 * @author cdennis
 */
public abstract class AbstractRejoinTest extends AbstractStandaloneTest {

  public AbstractRejoinTest(TestConfig testConfig, Class<? extends AbstractClientBase>... classes) {
    super(testConfig, classes);

    testConfig.setNumOfGroups(1);
    testConfig.addTcProperty("l2.l1reconnect.enabled", "false");
    TestBaseUtil.enabledL1ProxyConnection(testConfig);
  }
  
  public static abstract class AbstractRejoinClient extends ClientBase {

    public AbstractRejoinClient(String[] args) {
      super(args);
    }
    
    @Override
    public void addSchedulerProperties(Properties properties) {
      super.addSchedulerProperties(properties);
      properties.remove(AbstractTerracottaJobStore.TC_CONFIGURL_PROP);
      try {
        properties.setProperty(AbstractTerracottaJobStore.TC_CONFIG_PROP, getTestControlMbean().getTsaProxyTcConfig());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      properties.setProperty(AbstractTerracottaJobStore.TC_REJOIN_PROP, "true");
    }

    @Override
    protected boolean isSynchWrite() {
      return true;
    }
    
    @Override
    public String getTerracottaUrl() {
      return getTestControlMbean().getTsaProxyTerracottaUrl();
    }
    
    @Override
    public Properties getToolkitProps() {
      Properties props = super.getToolkitProps();
      props.setProperty("rejoin", "true");
      try {
        props.setProperty("tcConfigSnippet", getTestControlMbean().getTsaProxyTcConfig());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      return props;
    }

    public void initiateRejoin() throws Exception {
      getTestControlMbean().stopTsaProxy(0);
      getTestControlMbean().startTsaProxy(0);
    }
    
    public void completeRejoin() throws Exception {
      getTestControlMbean().startTsaProxy(0);
    }
  }
}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quartz.integrations.tests;

import org.quartz.simpl.CascadingClassLoadHelper;


/**
 * 
 * Always mimic CascadingClassLoadHelper except...
 * when looking for OnlyVisibleByDummyClassLoadHelperJob
 * 
 * @author adahanne
 *
 */
public class DummyClassLoadHelper extends CascadingClassLoadHelper {
  
  @Override
  public Class<?> loadClass(String className) throws ClassNotFoundException {
	  if(className.equals("imaginary.class.OnlyVisibleByDummyClassLoadHelperJob")){
		  //we just translate the className ! Only DummyClassLoadHelper is able to do that !
		  className = "org.quartz.integrations.tests.HelloJob";
	  }
	  return super.loadClass(className);
	  
  }

}
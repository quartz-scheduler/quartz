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

package org.terracotta.quartz.upgradability.serialization;

import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author cdennis
 */
public class SimplisticTimeZone extends TimeZone {

  public SimplisticTimeZone(String id) {
    setID(id);
  }
  
  @Override
  public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
    return 0;
  }

  @Override
  public void setRawOffset(int offsetMillis) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getRawOffset() {
    return 0;
  }

  @Override
  public boolean useDaylightTime() {
    return false;
  }

  @Override
  public boolean inDaylightTime(Date date) {
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SimplisticTimeZone) {
      return getID().equals(((TimeZone) obj).getID());
    } else {
      return false;
    }
  }
  
  
}

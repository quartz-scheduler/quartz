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
package org.quartz.impl.jdbcjobstore;

import java.math.BigDecimal;

public class SimplePropertiesTriggerProperties {

    private String string1;
    private String string2;
    private String string3;
    
    private int int1;
    private int int2;

    private long long1;
    private long long2;
    
    private BigDecimal decimal1;
    private BigDecimal decimal2;
    
    private boolean boolean1;
    private boolean boolean2;
    
    
    public String getString1() {
        return string1;
    }
    public void setString1(String string1) {
        this.string1 = string1;
    }
    public String getString2() {
        return string2;
    }
    public void setString2(String string2) {
        this.string2 = string2;
    }
    public String getString3() {
        return string3;
    }
    public void setString3(String string3) {
        this.string3 = string3;
    }
    public int getInt1() {
        return int1;
    }
    public void setInt1(int int1) {
        this.int1 = int1;
    }
    public int getInt2() {
        return int2;
    }
    public void setInt2(int int2) {
        this.int2 = int2;
    }
    public long getLong1() {
        return long1;
    }
    public void setLong1(long long1) {
        this.long1 = long1;
    }
    public long getLong2() {
        return long2;
    }
    public void setLong2(long long2) {
        this.long2 = long2;
    }
    public BigDecimal getDecimal1() {
        return decimal1;
    }
    public void setDecimal1(BigDecimal decimal1) {
        this.decimal1 = decimal1;
    }
    public BigDecimal getDecimal2() {
        return decimal2;
    }
    public void setDecimal2(BigDecimal decimal2) {
        this.decimal2 = decimal2;
    }
    public boolean isBoolean1() {
        return boolean1;
    }
    public void setBoolean1(boolean boolean1) {
        this.boolean1 = boolean1;
    }
    public boolean isBoolean2() {
        return boolean2;
    }
    public void setBoolean2(boolean boolean2) {
        this.boolean2 = boolean2;
    }

    
}

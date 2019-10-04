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

package weblogic.jdbc.vendor.oracle;

import java.io.OutputStream;
import java.sql.SQLException;

public interface OracleThinBlob {

  public abstract OutputStream getBinaryOutputStream() throws SQLException;

  public abstract int getBufferSize() throws SQLException;

  public abstract int getChunkSize() throws SQLException;

  public abstract int putBytes(long l, byte abyte0[]) throws SQLException;

  public abstract void truncate(long l) throws SQLException;

  public abstract void trim(long l) throws SQLException;
}

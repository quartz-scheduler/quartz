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

package org.quartz.jobs;

import java.io.File;

/**
 * Interface for objects wishing to receive a 'call-back' from a 
 * <code>DirectoryScanJob</code>.
 * 
 * <p>Instances should be stored in the {@link org.quartz.SchedulerContext} 
 * such that the <code>DirectoryScanJob</code> can find it.</p>
 * 
 * @author jhouse
 * @see org.quartz.jobs.DirectoryScanJob
 * @see org.quartz.SchedulerContext
 */
public interface DirectoryScanListener {

    /**
     * @param updatedFiles The set of files that were updated/added since the
     * last scan of the directory
     */
    void filesUpdatedOrAdded(File[] updatedFiles);
}

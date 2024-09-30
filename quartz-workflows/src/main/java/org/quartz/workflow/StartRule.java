/*
 * Copyright 2023 Dimitry Polivaev, Unite
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.quartz.workflow;

class StartRule implements WorkflowRule {
    private final Runnable runnable;
    
    private static final long serialVersionUID = 1L;

    StartRule(Runnable runnable) {
        super();
        this.runnable = runnable;
    }

    @Override
    public void verify(Schedulers schedulers) {/**/}

    @Override
    public String description() {
        return StartRule.class.getSimpleName();
    }

    @Override
    public void apply(RuleParameters ruleParameters, Schedulers schedulers){
        runnable.run();
    }

    private void writeObject(@SuppressWarnings("unused") java.io.ObjectOutputStream out){
        throw new UnsupportedOperationException(); 
    }
    
    private void readObject(@SuppressWarnings("unused") java.io.ObjectInputStream in){
        throw new UnsupportedOperationException(); 
    }
 }
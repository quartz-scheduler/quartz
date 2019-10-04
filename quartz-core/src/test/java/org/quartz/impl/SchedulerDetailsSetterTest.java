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
package org.quartz.impl;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.quartz.SchedulerException;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.ThreadPool;

public class SchedulerDetailsSetterTest extends TestCase {

    public void testSetter() throws SchedulerException, IOException {
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("/org/quartz/quartz.properties"));
        props.setProperty(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, MyThreadPool.class.getName());
        props.setProperty(StdSchedulerFactory.PROP_JOB_STORE_CLASS, MyJobStore.class.getName());
        
        StdSchedulerFactory factory = new StdSchedulerFactory(props);
        factory.getScheduler(); // this will initialize all the test fixtures.
        
        assertEquals(3, instanceIdCalls.get());
        assertEquals(3, instanceNameCalls.get());

        DirectSchedulerFactory directFactory = DirectSchedulerFactory.getInstance();
        directFactory.createScheduler(new MyThreadPool(), new MyJobStore());

        assertEquals(5, instanceIdCalls.get());
        assertEquals(6, instanceNameCalls.get());
    }

    public void testMissingSetterMethods() throws SchedulerException  {
        SchedulerDetailsSetter.setDetails(new Object(), "name", "id");
    }

    public void testUnimplementedMethods() throws Exception {
        ThreadPool tp = makeIncompleteThreadPool();
        try {
            tp.setInstanceName("name");
            fail();
        } catch (AbstractMethodError ame) {
            // expected
        }

        SchedulerDetailsSetter.setDetails(tp, "name", "id");
    }


    private ThreadPool makeIncompleteThreadPool() throws InstantiationException, IllegalAccessException {
        String name = "IncompleteThreadPool";
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", new String[] { "org/quartz/spi/ThreadPool" });

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();

        return (ThreadPool) new ClassLoader() {
            Class<?> defineClass(String clname, byte[] b) {
                return defineClass(clname, b, 0, b.length);
            }
        }.defineClass(name, cw.toByteArray()).newInstance();
    }

    private static final AtomicInteger instanceIdCalls = new AtomicInteger();
    private static final AtomicInteger instanceNameCalls = new AtomicInteger();

    public static class MyThreadPool extends SimpleThreadPool {
        
        @Override
        public void initialize() {
        }

        @Override
        public void setInstanceId(String schedInstId) {
            super.setInstanceId(schedInstId);
            instanceIdCalls.incrementAndGet();
        }

        @Override
        public void setInstanceName(String schedName) {
            super.setInstanceName(schedName);
            instanceNameCalls.incrementAndGet();
        }
    }


    public static class MyJobStore extends RAMJobStore {

        @Override
        public void setInstanceId(String schedInstId) {
            super.setInstanceId(schedInstId);
            instanceIdCalls.incrementAndGet();
        }

        @Override
        public void setInstanceName(String schedName) {
            super.setInstanceName(schedName);
            instanceNameCalls.incrementAndGet();
        }
    }

}

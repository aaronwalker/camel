/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.test.junit4;

import java.util.concurrent.TimeUnit;

import org.apache.camel.test.spring.ShutdownTimeout;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

@ShutdownTimeout(value = 11, timeUnit = TimeUnit.MILLISECONDS)
public class CamelSpringJUnit4ClassRunnerShutdownTimeoutTest
        extends CamelSpringJUnit4ClassRunnerPlainTest {

    @Test
    @Override
    public void testShutdownTimeout() throws Exception {
        assertEquals(11, camelContext.getShutdownStrategy().getTimeout());
        assertEquals(TimeUnit.MILLISECONDS, camelContext.getShutdownStrategy().getTimeUnit());
    }
}

/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
 * %%
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
 * #L%
 */
package com.adobe.acs.commons.util.impl.backwardscompatability;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class LegacyConfigTest {

    @Test
    public void handleLegacyConfig() {
        Test1 impl = new Test1() {
            @Override
            public String fromLegacy() {
                return "new value";
            }

            @Override
            public String noLegacy() {
                return "also a new value";
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Test1.class;
            }
        };
        Test1 proxied = LegacyConfig.handleLegacyConfig(impl, Collections.singletonMap("legacy", "old value"));
        assertEquals("old value", proxied.fromLegacy());
        assertEquals("also a new value", proxied.noLegacy());

    }

    public @interface Test1 {
        @LegacyName("legacy")
        String fromLegacy();

        String noLegacy();
    }
}
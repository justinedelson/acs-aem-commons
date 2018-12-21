/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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

import com.adobe.cq.commerce.common.ValueMapDecorator;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.ComponentContext;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LegacyConfig {

    private LegacyConfig() {
    }

    private static <T> T handleLegacyConfig(T config, ValueMap legacy) {
        Class[] interfaces = config.getClass().getInterfaces();
        if (interfaces.length == 0) {
            throw new IllegalArgumentException("Can only handle legacy configuration for implementations of an interface");
        }

        return (T) Proxy.newProxyInstance(config.getClass().getClassLoader(), new Class[] { interfaces[0] }, (object, method, args) -> {
            LegacyName legacyName = method.getAnnotation(LegacyName.class);
            if (legacyName == null) {
                return method.invoke(config, args);
            } else {
                return legacy.get(legacyName.value(), method.invoke(config, args));
            }
        });
    }

    public static <T> T handleLegacyConfig(T config, Map<String, Object> legacy) {
        return handleLegacyConfig(config, new ValueMapDecorator(legacy));

    }

    public static <T> T handleLegacyConfig(T config, ComponentContext componentContext) {
        Dictionary<String, Object> dictionary = componentContext.getProperties();
        final List<String> keys = Collections.list(dictionary.keys());
        ValueMap legacy = new ValueMapDecorator(keys.stream().collect(Collectors.toMap(Function.identity(), dictionary::get)));
        return handleLegacyConfig(config, legacy);
    }

}

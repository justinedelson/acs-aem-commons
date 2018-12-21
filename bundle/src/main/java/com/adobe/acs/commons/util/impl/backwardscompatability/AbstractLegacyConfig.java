package com.adobe.acs.commons.util.impl.backwardscompatability;

import com.adobe.cq.commerce.common.ValueMapDecorator;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.ComponentContext;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractLegacyConfig<T> {

    protected final ValueMap legacy;

    protected AbstractLegacyConfig(final Map<String, Object> legacy) {
        this.legacy = new ValueMapDecorator(legacy);
    }

    protected AbstractLegacyConfig(final ComponentContext componentContext) {
        Dictionary<String, Object> dictionary = componentContext.getProperties();
        final List<String> keys = Collections.list(dictionary.keys());
        this.legacy = new ValueMapDecorator(keys.stream().collect(Collectors.toMap(Function.identity(), dictionary::get)));
    }

}

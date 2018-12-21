package com.adobe.acs.commons.util.impl.backwardscompatability;

import com.adobe.acs.commons.errorpagehandler.impl.ErrorPageHandlerImpl;
import org.osgi.service.component.ComponentContext;

public class ErrorPageHandlerImplConfig extends AbstractLegacyConfig implements ErrorPageHandlerImpl.Config {
    private ErrorPageHandlerImpl.Config config;

    public ErrorPageHandlerImplConfig(ErrorPageHandlerImpl.Config config, ComponentContext legacy) {
        super(legacy);
        this.config = config;
    }

    @Override
    public boolean enabled() {
        return config.enabled();
    }

    @Override
    public boolean vanity_dispatch_enabled() {
        return false;
    }

    @Override
    public String errorpage_extension() {
        return legacy.get("error-page.extension", config.errorpage_extension());
    }

    @Override
    public String errorpage_fallbackname() {
        return legacy.get("error-page.fallback-name", config.errorpage_fallbackname());
    }

    @Override
    public String errorpage_systempath() {
        return legacy.get("error-page.system-path", config.errorpage_systempath());
    }

    @Override
    public String[] paths() {
        return config.paths();
    }

    @Override
    public String notfound_behavior() {
        return legacy.get("not-found.behavior", config.notfound_behavior());
    }

    @Override
    public String[] notfound_exclusionpathpatterns() {
        return legacy.get("not-found.exclusion-path-patterns", config.notfound_exclusionpathpatterns());
    }

    @Override
    public boolean cache_serveauthenticated() {
        return legacy.get("cache.serve-authenticated", config.cache_serveauthenticated());
    }

    @Override
    public int cache_ttl() {
        return config.cache_ttl();
    }

    @Override
    public boolean errorimages_enabled() {
        return legacy.get("error-images.enabled", config.errorimages_enabled());
    }

    @Override
    public String errorimages_path() {
        return legacy.get("error-images.path", config.errorimages_path());
    }

    @Override
    public String[] errorimages_extensions() {
        return legacy.get("error-images.extensions", config.errorimages_extensions());
    }
}

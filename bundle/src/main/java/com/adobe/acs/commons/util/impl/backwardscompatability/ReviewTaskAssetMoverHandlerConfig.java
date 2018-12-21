package com.adobe.acs.commons.util.impl.backwardscompatability;

import com.adobe.acs.commons.dam.impl.ReviewTaskAssetMoverHandler;

import java.util.Map;

public class ReviewTaskAssetMoverHandlerConfig extends AbstractLegacyConfig implements ReviewTaskAssetMoverHandler.Config {
    private ReviewTaskAssetMoverHandler.Config config;

    public ReviewTaskAssetMoverHandlerConfig(ReviewTaskAssetMoverHandler.Config config, Map<String, Object> legacy) {
        super(legacy);
        this.config = config;
    }

    @Override
    public String conflict_resolution_default() {
        return legacy.get("conflict-resolution.default", config.conflict_resolution_default());
    }

    @Override
    public String conflict_resolution_version_last_modified_by() {
        return legacy.get("conflict-resolution.version.last-modified-by", config.conflict_resolution_version_last_modified_by());
    }
}

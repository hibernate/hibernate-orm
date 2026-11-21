package org.hibernate.orm.test.refresh;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		@Setting(name = AvailableSettings.CACHE_REGION_FACTORY, value = "org.hibernate.testing.cache.CachingRegionFactory")
})
class NestedCascadeRefreshWithCacheTest extends NestedCascadeRefreshTest {
}

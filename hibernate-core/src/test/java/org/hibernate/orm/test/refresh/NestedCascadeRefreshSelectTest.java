package org.hibernate.orm.test.refresh;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false"),
		@Setting(name = AvailableSettings.MAX_FETCH_DEPTH, value = "0")
})
class NestedCascadeRefreshSelectTest extends NestedCascadeRefreshTest {
}

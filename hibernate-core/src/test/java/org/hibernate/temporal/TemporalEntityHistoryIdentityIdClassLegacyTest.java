package org.hibernate.temporal;

import org.hibernate.cfg.StateManagementSettings;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;

/**
 * {@link TemporalEntityHistoryIdentityIdClassTest} with the legacy action queue.
 */
@ServiceRegistry(settings = {
		@Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = FLUSH_QUEUE_TYPE, value = "legacy")
})
class TemporalEntityHistoryIdentityIdClassLegacyTest extends TemporalEntityHistoryIdentityIdClassTest {
}

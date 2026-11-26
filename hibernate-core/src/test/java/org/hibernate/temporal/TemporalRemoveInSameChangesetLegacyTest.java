package org.hibernate.temporal;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;

@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"),
		@Setting(name = FLUSH_QUEUE_TYPE, value = "legacy")
})
class TemporalRemoveInSameChangesetLegacyTest extends TemporalRemoveInSameChangesetTest {
}

package org.hibernate.temporal;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.JdbcSettings.USE_GET_GENERATED_KEYS;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;

@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = USE_GET_GENERATED_KEYS, value = "true"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = TemporalGeneratedPropertyHistoryTest.SUPPLIER)
})
class TemporalGeneratedUpsertHistoryWithKeysTest extends TemporalGeneratedUpsertHistoryTest {
}

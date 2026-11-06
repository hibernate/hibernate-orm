package org.hibernate.temporal;

import org.hibernate.cfg.BatchSettings;
import org.hibernate.cfg.StateManagementSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

/**
 * {@link TemporalEntityHistoryIdentityIdTest} with JDBC batching enabled, so the
 * history table mutations go through the batched mutation executor.
 */
@ServiceRegistry(settings = {
		@Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = BatchSettings.STATEMENT_BATCH_SIZE, value = "5")
})
@JiraKey("HHH-21060")
class TemporalEntityHistoryIdentityIdBatchedTest extends TemporalEntityHistoryIdentityIdTest {
}

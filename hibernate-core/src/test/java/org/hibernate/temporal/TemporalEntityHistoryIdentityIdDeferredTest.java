package org.hibernate.temporal;

import org.hibernate.cfg.StateManagementSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.FlushSettings.GRAPH_DEFER_IDENTITY_INSERTS;

/**
 * {@link TemporalEntityHistoryIdentityIdTest} with the graph action queue planning
 * the {@code IDENTITY} inserts, so the history row of an insert is written with an
 * identifier which is generated when the flush executes it.
 */
@ServiceRegistry(settings = {
		@Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = GRAPH_DEFER_IDENTITY_INSERTS, value = "true")
})
@JiraKey("HHH-21255")
class TemporalEntityHistoryIdentityIdDeferredTest extends TemporalEntityHistoryIdentityIdTest {
}

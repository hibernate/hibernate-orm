package org.hibernate.temporal;

import org.hibernate.dialect.generated.spi.GeneratedValuesSupport;
import org.hibernate.orm.test.secondarytable.OptionalSecondaryRowFallbackTest.FallbackDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;

@ServiceRegistry(settings = {
		@Setting(name = DIALECT, value = "org.hibernate.temporal.TemporalGeneratedUpsertHistoryFallbackTest$NoReturningDialect"),
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = TemporalGeneratedPropertyHistoryTest.SUPPLIER)
})
public class TemporalGeneratedUpsertHistoryFallbackTest extends TemporalGeneratedUpsertHistoryTest {
	public static class NoReturningDialect extends FallbackDialect {
		@Override
		public GeneratedValuesSupport getGeneratedValuesSupport() {
			return GeneratedValuesSupport.STANDARD;
		}
	}

	@Override
	int expectedFollowUpSelects() {
		return 1;
	}
}

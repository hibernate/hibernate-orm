package org.hibernate.orm.test.locking;

import org.hibernate.dialect.generated.spi.GeneratedValuesSupport;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.dialect.generated.spi.GeneratedValuesSupport.Capability.ARBITRARY_GENERATED_KEYS;

@ServiceRegistry(settings = @Setting(name = DIALECT,
		value = "org.hibernate.orm.test.locking.ExcludedGeneratedVersioningSelectingTests$SelectingDialect"))
public class ExcludedGeneratedVersioningSelectingTests extends ExcludedGeneratedVersioningNoReturningTests {
	public static class SelectingDialect extends ExcludedFromVersioningNoReturningTests.NoReturningDialect {
		@Override
		public GeneratedValuesSupport getGeneratedValuesSupport() {
			return GeneratedValuesSupport.builder( super.getGeneratedValuesSupport() )
					.disable( ARBITRARY_GENERATED_KEYS )
					.unquoteGeneratedKeyColumnNames( false )
					.build();
		}
	}
}

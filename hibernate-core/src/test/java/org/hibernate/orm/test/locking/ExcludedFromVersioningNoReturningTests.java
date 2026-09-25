package org.hibernate.orm.test.locking;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.generated.spi.GeneratedValuesSupport;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.dialect.generated.spi.GeneratedValuesSupport.Capability.UPDATE_RETURNING;

/// Exercise version selection even when the test database supports update returning.
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = @Setting(name = DIALECT,
		value = "org.hibernate.orm.test.locking.ExcludedFromVersioningNoReturningTests$NoReturningDialect"))
public class ExcludedFromVersioningNoReturningTests extends ExcludedFromVersioningTests {
	public static class NoReturningDialect extends H2Dialect {
		@Override
		public GeneratedValuesSupport getGeneratedValuesSupport() {
			return GeneratedValuesSupport.builder( super.getGeneratedValuesSupport() )
					.disable( UPDATE_RETURNING )
					.build();
		}
	}
}

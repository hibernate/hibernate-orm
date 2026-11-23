package org.hibernate.orm.test.locking;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.JdbcSettings.DIALECT;

/// Executes the collection-acquisition suite with real JDBC bindings and row
/// locks while forcing tuple-predicate emulation in the SQL translator.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = {
		@Setting(name = DIALECT, value = "org.hibernate.orm.test.locking.CompositeCollectionLockingEmulationTest$NoTuplesDialect"),
		@Setting(name = "hibernate.connection.isolation", value = "2"),
		@Setting(name = "hibernate.use_sql_comments", value = "false")
})
class CompositeCollectionLockingEmulationTest extends CompositeCollectionLockingTest {
	@AfterEach
	void verifiesEmulatedCollectionPredicates(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getJdbcServices().getDialect().getRowValueSupport() ).isSameAs( RowValueSupport.NONE );
		scope.getCollectingStatementObserver().getSqlQueries().stream()
				.filter( CompositeCollectionLockingTest::collectionSelect )
				.forEach( sql -> assertThat( sql ).doesNotContain( " in (" ) );
	}

	public static class NoTuplesDialect extends H2Dialect {
		public NoTuplesDialect(DialectResolutionInfo info) {
			super( info );
		}

		@Override
		public RowValueSupport getRowValueSupport() {
			return RowValueSupport.NONE;
		}
	}
}

package org.hibernate.orm.test.dialect.unit.locking;

import java.util.stream.Stream;

import org.hibernate.LockMode;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.lock.spi.LockingSupport;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.lock.spi.StandardLockingSupports.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

/// Locks down the explicit stock refresh policies and their legacy/version paths.
/// Functional tests independently verify the state and SQL consequences.
///
/// @author Steve Ebersole
class RefreshLockingPolicyTest {
	static Stream<Arguments> profiles() {
		return Stream.of(
				Arguments.of( "standard", standard(), Policy.PLAIN ),
				Arguments.of( "standard-no-outer", standardWithoutOuterJoinLocking(), Policy.PLAIN ),
				Arguments.of( "none", none(), Policy.RETAIN ),
				Arguments.of( "db2-11.4", db2( v( 11, 4 ) ), Policy.PLAIN ),
				Arguments.of( "db2-11.5", db2( v( 11, 5 ) ), Policy.PLAIN ),
				Arguments.of( "db2i", db2i(), Policy.PLAIN ),
				Arguments.of( "db2i-legacy", legacyDb2i(), Policy.PLAIN ),
				Arguments.of( "db2z", db2z(), Policy.PLAIN ),
				Arguments.of( "h2-old", h2( v( 2, 2, 219 ) ), Policy.PLAIN ),
				Arguments.of( "h2-current", h2( v( 2, 2, 220 ) ), Policy.PLAIN ),
				Arguments.of( "hana-no-wait", hana( v( 2, 0, 9 ) ), Policy.PLAIN ),
				Arguments.of( "hana-wait", hana( v( 2, 0, 10 ) ), Policy.PLAIN ),
				Arguments.of( "hana-skip", hana( v( 2, 0, 30 ) ), Policy.PLAIN ),
				Arguments.of( "hsql-no-clause", hsql( v( 1, 8 ) ), Policy.OWN_WRITE ),
				Arguments.of( "hsql-clause", hsql( v( 2 ) ), Policy.PLAIN ),
				Arguments.of( "mysql-old", mysql( v( 5, 7 ) ), Policy.RETAIN ),
				Arguments.of( "mysql-current", mysql( v( 8 ) ), Policy.RETAIN ),
				Arguments.of( "maria-old", mariaDb( v( 10, 5 ) ), Policy.RETAIN ),
				Arguments.of( "maria-skip", mariaDb( v( 10, 6 ) ), Policy.RETAIN ),
				Arguments.of( "maria-snapshot-default", mariaDb( v( 11, 6, 2 ) ), Policy.RETAIN ),
				Arguments.of( "oracle-old", oracle( v( 9 ) ), Policy.PLAIN ),
				Arguments.of( "oracle-current", oracle( v( 23 ) ), Policy.PLAIN ),
				Arguments.of( "postgres-old", postgresql( v( 8 ) ), Policy.PLAIN ),
				Arguments.of( "postgres-nowait", postgresql( v( 8, 1 ) ), Policy.PLAIN ),
				Arguments.of( "postgres-skip", postgresql( v( 9, 5 ) ), Policy.PLAIN ),
				Arguments.of( "cockroach-no-clause", cockroach( v( 20 ) ), Policy.OWN_WRITE ),
				Arguments.of( "cockroach-clause", cockroach( v( 20, 1 ) ), Policy.PLAIN ),
				Arguments.of( "sqlserver-old", sqlServer( v( 8 ) ), Policy.PLAIN ),
				Arguments.of( "sqlserver-current", sqlServer( v( 16 ) ), Policy.PLAIN ),
				Arguments.of( "sybase", sybase(), Policy.PLAIN ),
				Arguments.of( "sybase-ase", sybaseAse(), Policy.PLAIN ),
				Arguments.of( "sybase-ase-select-for-update-disabled",
						org.hibernate.dialect.lock.internal.TransactSQLLockingSupport.forSybaseASE( false ), Policy.PLAIN ),
				Arguments.of( "sybase-ase-legacy", legacySybaseAse(), Policy.PLAIN ),
				Arguments.of( "sybase-anywhere-old", sybaseAnywhere( v( 9 ) ), Policy.PLAIN ),
				Arguments.of( "sybase-anywhere-current", sybaseAnywhere( v( 10 ) ), Policy.PLAIN ),
				Arguments.of( "times-ten", timesTen(), Policy.PLAIN ),
				Arguments.of( "independent-provider-default", mock( LockingSupport.class, CALLS_REAL_METHODS ), Policy.RETAIN ),
				Arguments.of( "spanner", new org.hibernate.dialect.SpannerDialect().getLockingSupport(), Policy.PLAIN ),
				Arguments.of( "spanner-postgresql", new org.hibernate.dialect.SpannerPostgreSQLDialect().getLockingSupport(), Policy.PLAIN ),
				Arguments.of( "disabled-simple", simple( org.hibernate.dialect.lock.PessimisticLockStyle.NONE,
						org.hibernate.dialect.lock.spi.RowLockStrategy.NONE, org.hibernate.dialect.lock.spi.LockTimeoutType.NONE,
						org.hibernate.dialect.lock.spi.OuterJoinLockingType.UNSUPPORTED,
						org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy.NONE ), Policy.RETAIN )
		);
	}

	@ParameterizedTest(name = "{0}: {2}")
	@MethodSource("profiles")
	void classifiesEveryEligibleModePair(String name, LockingSupport support, Policy policy) {
		for ( var held : LockMode.values() ) {
			if ( held.isPessimistic() ) {
				for ( var requested : LockMode.values() ) {
					if ( requested != LockMode.WRITE && !requested.greaterThan( held ) ) {
						final boolean expected = policy == Policy.PLAIN
								|| policy == Policy.OWN_WRITE && held == LockMode.PESSIMISTIC_FORCE_INCREMENT;
						assertThat( support.canUsePlainReadForRefresh( held, requested ) )
								.as( "%s: %s -> %s", name, held, requested ).isEqualTo( expected );
					}
				}
			}
		}
	}

	enum Policy { PLAIN, OWN_WRITE, RETAIN }

	private static DatabaseVersion v(int major, int minor, int micro) {
		return DatabaseVersion.make( major, minor, micro );
	}

	private static DatabaseVersion v(int major, int minor) { return v( major, minor, 0 ); }
	private static DatabaseVersion v(int major) { return v( major, 0 ); }
}

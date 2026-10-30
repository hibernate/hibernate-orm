package org.hibernate.community.dialect;

import java.util.stream.Stream;

import org.hibernate.LockMode;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.lock.spi.LockingSupport;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies that community and legacy dialect delegation selects the audited
/// refresh policy, including version boundaries and disabled locking profiles.
///
/// @author Steve Ebersole
class RefreshLockingPolicyTest {
	static Stream<Arguments> profiles() {
		return Stream.of(
				profile( new H2LegacyDialect( v( 2, 2, 219 ) ), Policy.PLAIN ),
				profile( new H2LegacyDialect( v( 2, 2, 220 ) ), Policy.PLAIN ),
				profile( new HSQLLegacyDialect( org.hibernate.orm.test.dialect.resolver.TestingDialectResolutionInfo.forDatabaseInfo( "HSQL Database Engine", 1, 8 ) ), Policy.OWN_WRITE ),
				profile( new HSQLLegacyDialect( v( 2 ) ), Policy.PLAIN ),
				profile( new MySQLLegacyDialect( v( 5, 7 ) ), Policy.RETAIN ),
				profile( new MySQLLegacyDialect( v( 8 ) ), Policy.RETAIN ),
				profile( new PostgreSQLLegacyDialect( v( 8 ) ), Policy.PLAIN ),
				profile( new PostgreSQLLegacyDialect( v( 9, 5 ) ), Policy.PLAIN ),
				profile( new OracleLegacyDialect( v( 9 ) ), Policy.PLAIN ),
				profile( new OracleLegacyDialect( v( 23 ) ), Policy.PLAIN ),
				profile( new DB2LegacyDialect( v( 11, 1 ) ), Policy.PLAIN ),
				profile( new DB2LegacyDialect( v( 11, 5 ) ), Policy.PLAIN ),
				profile( new DB2iLegacyDialect(), Policy.PLAIN ),
				profile( new DB2zLegacyDialect(), Policy.PLAIN ),
				profile( new HANALegacyDialect( v( 2, 0, 10 ) ), Policy.PLAIN ),
				profile( new HANALegacyDialect( v( 2, 0, 30 ) ), Policy.PLAIN ),
				profile( new SQLServerLegacyDialect( v( 8 ) ), Policy.PLAIN ),
				profile( new SQLServerLegacyDialect( v( 16 ) ), Policy.PLAIN ),
				profile( new SybaseLegacyDialect(), Policy.PLAIN ),
				profile( new SybaseASELegacyDialect(), Policy.PLAIN ),
				profile( new SybaseAnywhereDialect( v( 9 ) ), Policy.PLAIN ),
				profile( new SybaseAnywhereDialect( v( 10 ) ), Policy.PLAIN ),
				profile( new CockroachLegacyDialect( v( 20 ) ), Policy.OWN_WRITE ),
				profile( new CockroachLegacyDialect( v( 20, 1 ) ), Policy.PLAIN ),
				profile( new TiDBDialect(), Policy.RETAIN ),
				profile( new InformixDialect(), Policy.OWN_WRITE ),
				profile( new GaussDBDialect(), Policy.PLAIN ),
				profile( new FirebirdDialect( v( 4 ) ), Policy.PLAIN ),
				profile( new FirebirdDialect( v( 5 ) ), Policy.PLAIN ),
				profile( new DerbyDialect(), Policy.PLAIN ),
				profile( new DerbyLegacyDialect(), Policy.PLAIN ),
				profile( new TimesTenDialect(), Policy.PLAIN ),
				profile( new TeradataDialect( v( 13 ) ), Policy.PLAIN ),
				profile( new TeradataDialect( v( 14 ) ), Policy.PLAIN ),
				profile( new AltibaseDialect(), Policy.PLAIN ),
				profile( new CUBRIDDialect(), Policy.PLAIN ),
				profile( new MaxDBDialect(), Policy.PLAIN ),
				profile( new MimerSQLDialect(), Policy.PLAIN ),
				profile( new SingleStoreDialect( v( 8 ), null, true ), Policy.PLAIN ),
				profile( new SingleStoreDialect( v( 8 ), null, false ), Policy.RETAIN ),
				profile( new CacheDialect(), Policy.RETAIN ),
				profile( new IngresDialect(), Policy.RETAIN ),
				profile( new InterSystemsIRISDialect(), Policy.RETAIN ),
				profile( new RDMSOS2200Dialect(), Policy.RETAIN ),
				profile( new SQLiteDialect(), Policy.RETAIN )
		);
	}

	private static Arguments profile(org.hibernate.dialect.Dialect dialect, Policy policy) {
		return Arguments.of( dialect.getClass().getSimpleName() + " " + dialect.getVersion(), dialect.getLockingSupport(), policy );
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

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.EXISTS_IN_SELECT;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.IN_PREDICATE_LHS;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.LATERAL;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.MUTATION_JOIN;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.MUTATION_TARGET_REFERENCE;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.NESTED_CORRELATION;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.OFFSET;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.ORDER_BY;
import static org.hibernate.dialect.sql.ast.spi.SubquerySupport.Feature.SELECT_LIST;

/// Verifies every nonstandard or version-dependent community subquery profile.
///
/// @author Steve Ebersole
public class SubquerySupportTest {
	@Test
	void selectListExistsAndOrderingProfilesPreserveEveryOverride() {
		assertSupport( new IngresDialect( DatabaseVersion.make( 9 ) ), SELECT_LIST, false );
		assertSupport( new IngresDialect( DatabaseVersion.make( 10 ) ), SELECT_LIST, true );

		assertSupport( new DerbyDialect(), EXISTS_IN_SELECT, false );
		assertSupport( new DerbyLegacyDialect(), EXISTS_IN_SELECT, false );
		assertSupport( new CUBRIDDialect(), EXISTS_IN_SELECT, false );
		assertSupport( new FirebirdDialect( DatabaseVersion.make( 2, 5 ) ), EXISTS_IN_SELECT, false );
		assertSupport( new FirebirdDialect( DatabaseVersion.make( 3 ) ), EXISTS_IN_SELECT, true );
		assertSupport( new SQLServerLegacyDialect(), EXISTS_IN_SELECT, false );
		assertSupport( new HANALegacyDialect(), EXISTS_IN_SELECT, false );
		assertSupport( new DB2LegacyDialect(), EXISTS_IN_SELECT, false );
		assertSupport( new OracleLegacyDialect(), EXISTS_IN_SELECT, false );
		assertSupport( new InterSystemsIRISDialect(), EXISTS_IN_SELECT, false );
		assertSupport( new AltibaseDialect(), EXISTS_IN_SELECT, false );
		assertSupport( new TeradataDialect(), EXISTS_IN_SELECT, false );

		assertSupport( new DerbyLegacyDialect( DatabaseVersion.make( 10, 4 ) ), ORDER_BY, false );
		assertSupport( new DerbyLegacyDialect( DatabaseVersion.make( 10, 5 ) ), ORDER_BY, true );
		assertSupport( new HANALegacyDialect(), ORDER_BY, true );
		assertSupport( new CacheDialect(), ORDER_BY, false );
		assertSupport( new GaussDBDialect(), ORDER_BY, false );
		assertSupport( new InformixDialect(), ORDER_BY, false );
		assertSupport( new IngresDialect(), ORDER_BY, false );
		assertSupport( new InterSystemsIRISDialect(), ORDER_BY, false );
		assertSupport( new RDMSOS2200Dialect(), ORDER_BY, false );
		assertSupport( new SybaseASELegacyDialect(), ORDER_BY, false );
		assertSupport( new AltibaseDialect(), ORDER_BY, false );
		assertSupport( new TeradataDialect(), ORDER_BY, false );
	}

	@Test
	void offsetProfilesPreserveEveryCommunityOverride() {
		for ( Dialect dialect : new Dialect[] {
				new CUBRIDDialect(),
				new CockroachLegacyDialect(),
				new DB2LegacyDialect(),
				new FirebirdDialect(),
				new GaussDBDialect(),
				new H2LegacyDialect(),
				new HANALegacyDialect(),
				new HSQLLegacyDialect(),
				new MaxDBDialect(),
				new MimerSQLDialect(),
				new MySQLLegacyDialect(),
				new OracleLegacyDialect(),
				new PostgreSQLLegacyDialect(),
				new SQLServerLegacyDialect(),
				new SingleStoreDialect(),
				new TimesTenDialect()
		} ) {
			assertSupport( dialect, OFFSET, true );
		}
	}

	@Test
	void correlationAndMutationProfilesRemainIndependent() {
		assertSupport( new MySQLLegacyDialect(), NESTED_CORRELATION, false );
		assertSupport( new OracleLegacyDialect(), NESTED_CORRELATION, false );
		assertSupport( new SingleStoreDialect(), NESTED_CORRELATION, false );

		assertSupport( new MySQLLegacyDialect(), MUTATION_TARGET_REFERENCE, false );
		assertSupport( new InformixDialect( DatabaseVersion.make( 11, 50 ) ), MUTATION_TARGET_REFERENCE, false );
		assertSupport( new InformixDialect( DatabaseVersion.make( 12 ) ), MUTATION_TARGET_REFERENCE, true );
		assertSupport( new InterSystemsIRISDialect(), MUTATION_TARGET_REFERENCE, false );
		assertSupport( new SingleStoreDialect(), MUTATION_TARGET_REFERENCE, false );

		assertSupport( new DerbyDialect(), MUTATION_JOIN, false );
		assertSupport( new DerbyLegacyDialect(), MUTATION_JOIN, false );
		assertSupport( new H2LegacyDialect(), MUTATION_JOIN, false );
		assertSupport( new IngresDialect(), IN_PREDICATE_LHS, false );
		assertSupport( new InterSystemsIRISDialect(), IN_PREDICATE_LHS, false );
	}

	@Test
	void lateralProfilesPreserveEveryVersionBoundary() {
		assertBoundary( new DB2LegacyDialect( DatabaseVersion.make( 9 ) ), new DB2LegacyDialect( DatabaseVersion.make( 9, 1 ) ) );
		assertBoundary( new DB2iLegacyDialect( DatabaseVersion.make( 7 ) ), new DB2iLegacyDialect( DatabaseVersion.make( 7, 1 ) ) );
		assertSupport( new DB2zLegacyDialect(), LATERAL, true );
		assertBoundary( new HSQLLegacyDialect( DatabaseVersion.make( 2, 6 ) ), new HSQLLegacyDialect( DatabaseVersion.make( 2, 6, 1 ) ) );
		assertBoundary( new SQLServerLegacyDialect( DatabaseVersion.make( 8 ) ), new SQLServerLegacyDialect( DatabaseVersion.make( 9 ) ) );
		assertSupport( new InterSystemsIRISDialect(), LATERAL, true );
		assertSupport( new MariaDBLegacyDialect(), LATERAL, false );
		assertBoundary( new InformixDialect( DatabaseVersion.make( 12, 9 ) ), new InformixDialect( DatabaseVersion.make( 12, 10 ) ) );
		assertBoundary( new PostgreSQLLegacyDialect( DatabaseVersion.make( 9, 2 ) ), new PostgreSQLLegacyDialect( DatabaseVersion.make( 9, 3 ) ) );
		assertBoundary( new FirebirdDialect( DatabaseVersion.make( 3 ) ), new FirebirdDialect( DatabaseVersion.make( 4 ) ) );
		assertSupport( new GaussDBDialect(), LATERAL, false );
		assertBoundary( new CockroachLegacyDialect( DatabaseVersion.make( 20 ) ), new CockroachLegacyDialect( DatabaseVersion.make( 20, 1 ) ) );
		assertBoundary( new OracleLegacyDialect( DatabaseVersion.make( 12 ) ), new OracleLegacyDialect( DatabaseVersion.make( 12, 1 ) ) );
		assertBoundary( new HANALegacyDialect( DatabaseVersion.make( 2, 0, 39 ) ), new HANALegacyDialect( DatabaseVersion.make( 2, 0, 40 ) ) );
		assertBoundary( new SybaseAnywhereDialect( DatabaseVersion.make( 9 ) ), new SybaseAnywhereDialect( DatabaseVersion.make( 10 ) ) );
		assertBoundary( new MySQLLegacyDialect( DatabaseVersion.make( 8, 0, 13 ) ), new MySQLLegacyDialect( DatabaseVersion.make( 8, 0, 14 ) ) );
	}

	private static void assertBoundary(Dialect before, Dialect after) {
		assertSupport( before, LATERAL, false );
		assertSupport( after, LATERAL, true );
	}

	private static void assertSupport(Dialect dialect, SubquerySupport.Feature feature, boolean expected) {
		assertThat( dialect.getSubquerySupport().supports( feature ) )
				.as( "%s %s", dialect.getClass().getSimpleName(), feature )
				.isEqualTo( expected );
	}
}

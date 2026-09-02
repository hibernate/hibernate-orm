/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.query.sqm.SetOperator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.sql.ast.spi.SetOperationSupport.Capability.DUPLICATE_SELECT_ITEMS;
import static org.hibernate.dialect.sql.ast.spi.SetOperationSupport.Capability.SIMPLE_QUERY_GROUPING;
import static org.hibernate.dialect.sql.ast.spi.SetOperationSupport.Capability.UNION_IN_SUBQUERY;
import static org.hibernate.query.sqm.SetOperator.EXCEPT;
import static org.hibernate.query.sqm.SetOperator.INTERSECT;
import static org.hibernate.query.sqm.SetOperator.UNION;
import static org.hibernate.query.sqm.SetOperator.UNION_ALL;

/// Verifies the complete six-operator and three-capability matrix for every
/// community Dialect with nonstandard or version-dependent set grammar.
///
/// @author Steve Ebersole
public class SetOperationSupportTest {
	@Test
	void ingresAndMySqlFamiliesMatchTheirDocumentedVersionBoundaries() {
		// Ingres SELECT grammar has UNION [ALL], INTERSECT, and EXCEPT, but no ALL
		// form for INTERSECT or EXCEPT. Hibernate's pre-9.3 UNION ALL restriction
		// and UNION-subquery restriction are retained.
		assertProfile( new IngresDialect( DatabaseVersion.make( 9, 2 ) ), UNION, INTERSECT, EXCEPT );
		assertProfile( new IngresDialect( DatabaseVersion.make( 9, 3 ) ), UNION, UNION_ALL, INTERSECT, EXCEPT );
		assertCapabilities(
				new IngresDialect( DatabaseVersion.make( 9, 3 ) ),
				DUPLICATE_SELECT_ITEMS,
				SIMPLE_QUERY_GROUPING
		);

		// Legacy MySQL predates the 8.0.31 INTERSECT/EXCEPT grammar addition.
		assertProfile( new MySQLLegacyDialect( DatabaseVersion.make( 4 ) ), UNION );
		assertCapabilities(
				new MySQLLegacyDialect( DatabaseVersion.make( 4 ) ),
				DUPLICATE_SELECT_ITEMS
		);
		assertProfile( new MySQLLegacyDialect( DatabaseVersion.make( 5 ) ), UNION, UNION_ALL );
		assertCapabilities(
				new MySQLLegacyDialect( DatabaseVersion.make( 5 ) ),
				UNION_IN_SUBQUERY,
				DUPLICATE_SELECT_ITEMS
		);
		assertCapabilities(
				new MySQLLegacyDialect( DatabaseVersion.make( 8 ) ),
				UNION_IN_SUBQUERY,
				DUPLICATE_SELECT_ITEMS,
				SIMPLE_QUERY_GROUPING
		);

		// MariaDB documents INTERSECT/EXCEPT since 10.3 and ALL since 10.5.
		assertProfile( new MariaDBLegacyDialect( DatabaseVersion.make( 10, 2 ) ), UNION, UNION_ALL );
		assertProfile(
				new MariaDBLegacyDialect( DatabaseVersion.make( 10, 3 ) ),
				UNION,
				UNION_ALL,
				INTERSECT,
				EXCEPT
		);
		assertProfile( new MariaDBLegacyDialect( DatabaseVersion.make( 10, 5 ) ), SetOperator.values() );
		assertCapabilities(
				new MariaDBLegacyDialect( DatabaseVersion.make( 10, 3 ) ),
				UNION_IN_SUBQUERY
		);
		assertCapabilities(
				new MariaDBLegacyDialect( DatabaseVersion.make( 10, 5 ) ),
				UNION_IN_SUBQUERY,
				SIMPLE_QUERY_GROUPING
		);
	}

	@Test
	void firebirdInformixAndAseExposeOnlyTheirAuditedGrammar() {
		// Firebird 5 grammar documents only UNION [DISTINCT | ALL].
		assertProfile( new FirebirdDialect( DatabaseVersion.make( 4 ) ), UNION, UNION_ALL );
		assertCapabilities(
				new FirebirdDialect( DatabaseVersion.make( 4 ) ),
				UNION_IN_SUBQUERY,
				DUPLICATE_SELECT_ITEMS
		);
		assertCapabilities(
				new FirebirdDialect( DatabaseVersion.make( 5 ) ),
				UNION_IN_SUBQUERY,
				DUPLICATE_SELECT_ITEMS,
				SIMPLE_QUERY_GROUPING
		);

		// Informix documents ALL only for UNION; INTERSECT and EXCEPT arrive at
		// the existing 12.10 Hibernate boundary.
		assertProfile( new InformixDialect( DatabaseVersion.make( 12, 9 ) ), UNION, UNION_ALL );
		assertProfile(
				new InformixDialect( DatabaseVersion.make( 12, 10 ) ),
				UNION,
				UNION_ALL,
				INTERSECT,
				EXCEPT
		);
		assertCapabilities(
				new InformixDialect( DatabaseVersion.make( 12, 10 ) ),
				UNION_IN_SUBQUERY,
				DUPLICATE_SELECT_ITEMS,
				SIMPLE_QUERY_GROUPING
		);

		// Preserve Hibernate's established ASE INTERSECT and UNION-subquery
		// restrictions; ASE grammar has no ALL form for EXCEPT.
		assertProfile( new SybaseASELegacyDialect(), UNION, UNION_ALL, EXCEPT );
		assertCapabilities( new SybaseASELegacyDialect(), DUPLICATE_SELECT_ITEMS, SIMPLE_QUERY_GROUPING );
	}

	@Test
	void h2TidbOracleSqlServerAltibaseAndSingleStoreExposeAllSixDimensions() {
		// H2 2.4.240 parser verification rejects both non-UNION ALL forms.
		final H2LegacyDialect h2 = new H2LegacyDialect();
		assertProfile( h2, UNION, UNION_ALL, INTERSECT, EXCEPT );
		assertCapabilities( h2, UNION_IN_SUBQUERY, DUPLICATE_SELECT_ITEMS, SIMPLE_QUERY_GROUPING );

		// TiDB stable documentation explicitly excludes INTERSECT ALL and EXCEPT ALL.
		final TiDBDialect tiDB = new TiDBDialect();
		assertProfile( tiDB, UNION, UNION_ALL, INTERSECT, EXCEPT );
		assertCapabilities( tiDB, UNION_IN_SUBQUERY, DUPLICATE_SELECT_ITEMS, SIMPLE_QUERY_GROUPING );

		// Oracle 21 adds the ALL forms; legacy releases render EXCEPT as MINUS.
		final OracleLegacyDialect oracle19 = new OracleLegacyDialect( DatabaseVersion.make( 19 ) );
		assertProfile( oracle19, UNION, UNION_ALL, INTERSECT, EXCEPT );
		assertCapabilities( oracle19, UNION_IN_SUBQUERY, SIMPLE_QUERY_GROUPING );
		assertThat( oracle19.getSetOperatorSqlString( EXCEPT ) ).isEqualTo( "minus" );
		assertProfile( new OracleLegacyDialect( DatabaseVersion.make( 21 ) ), SetOperator.values() );

		// Transact-SQL grammar has ALL only for UNION.
		assertProfile( new SQLServerLegacyDialect(), UNION, UNION_ALL, INTERSECT, EXCEPT );
		assertCapabilities( new SQLServerLegacyDialect(), UNION_IN_SUBQUERY, DUPLICATE_SELECT_ITEMS );

		// Altibase 7.3 documents UNION, UNION ALL, INTERSECT, and MINUS.
		final AltibaseDialect altibase = new AltibaseDialect();
		assertProfile( altibase, UNION, UNION_ALL, INTERSECT, EXCEPT );
		assertCapabilities( altibase, UNION_IN_SUBQUERY, DUPLICATE_SELECT_ITEMS );
		assertThat( altibase.getSetOperatorSqlString( EXCEPT ) ).isEqualTo( "minus" );

		// SingleStore documents UNION [ALL], INTERSECT, and distinct EXCEPT/MINUS.
		final SingleStoreDialect singleStore = new SingleStoreDialect();
		assertProfile( singleStore, UNION, UNION_ALL, INTERSECT, EXCEPT );
		assertCapabilities(
				singleStore,
				UNION_IN_SUBQUERY,
				DUPLICATE_SELECT_ITEMS,
				SIMPLE_QUERY_GROUPING
		);
	}

	private static void assertProfile(Dialect dialect, SetOperator... operators) {
		assertThat( dialect.getSetOperationSupport().getSupportedOperators() )
				.containsExactlyInAnyOrder( operators );
	}

	private static void assertCapabilities(
			Dialect dialect,
			SetOperationSupport.Capability... capabilities) {
		assertThat( dialect.getSetOperationSupport().getCapabilities() )
				.containsExactlyInAnyOrder( capabilities );
	}
}

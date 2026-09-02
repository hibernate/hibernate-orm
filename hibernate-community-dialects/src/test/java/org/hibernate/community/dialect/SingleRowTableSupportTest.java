/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies every direct community single-row-table profile, inherited family
/// value, version boundary, and independent cross-join value.
///
/// @author Steve Ebersole
public class SingleRowTableSupportTest {
	@Test
	void namedAndDerivedCommunityProfilesPreserveExactPairs() {
		assertProfile( new AltibaseDialect(), "dual", " from dual" );
		assertProfile( new CUBRIDDialect(), "db_root", " from db_root" );
		assertProfile( new DerbyDialect(), "(values 0)", " from (values 0) dual" );
		assertProfile( new DerbyLegacyDialect(), "(values 0)", " from (values 0) dual" );
		assertProfile( new FirebirdDialect(), "rdb$database", " from rdb$database" );
		assertProfile( new H2LegacyDialect(), "dual", "" );
		assertProfile( new HANALegacyDialect(), "sys.dummy", " from sys.dummy" );
		assertProfile( new HSQLLegacyDialect(), "(values(0))", " from (values(0))" );
		assertProfile( new IngresDialect(), "(select 0)", " from (select 0) dual" );
		assertProfile( new InterSystemsIRISDialect(), "(select 1)", "" );
		assertProfile( new MaxDBDialect(), "dual", " from dual" );
		assertProfile( new MimerSQLDialect(), "(values(0))", " from (values(0))" );
		assertProfile( new OracleLegacyDialect(), "dual", " from dual" );
		assertProfile(
				new RDMSOS2200Dialect(),
				"rdms.rdms_dummy",
				" from rdms.rdms_dummy where key_col=1"
		);
		assertProfile( new SingleStoreDialect(), "dual", "" );
		assertProfile( new SybaseASELegacyDialect(), "(select 1 c1)", "" );
		assertProfile( new SybaseAnywhereDialect(), "sys.dummy", " from sys.dummy" );
		assertProfile( new TiDBDialect(), "dual", "" );
		assertProfile( new TimesTenDialect(), "dual", " from dual" );
	}

	@Test
	void db2LegacyDescendantsPreserveInheritedPair() {
		assertProfile( new DB2LegacyDialect(), "sysibm.sysdummy1", " from sysibm.sysdummy1" );
		assertProfile( new DB2iLegacyDialect(), "sysibm.sysdummy1", " from sysibm.sysdummy1" );
		assertProfile( new DB2zLegacyDialect(), "sysibm.sysdummy1", " from sysibm.sysdummy1" );
	}

	@Test
	void selectOnlyFragmentVersionTransitionsRemainExact() {
		assertProfile(
				new InformixDialect( DatabaseVersion.make( 12, 9 ) ),
				"(select 0 from systables where tabid=1)",
				" from (select 0 from systables where tabid=1) dual"
		);
		assertProfile(
				new InformixDialect( DatabaseVersion.make( 12, 10 ) ),
				"(select 0 from systables where tabid=1)",
				""
		);
		assertProfile( new MySQLLegacyDialect( DatabaseVersion.make( 7 ) ), "dual", " from dual" );
		assertProfile( new MySQLLegacyDialect( DatabaseVersion.make( 8 ) ), "dual", "" );
		assertProfile( new MariaDBLegacyDialect( DatabaseVersion.make( 10, 3 ) ), "dual", " from dual" );
		assertProfile( new MariaDBLegacyDialect( DatabaseVersion.make( 10, 4 ) ), "dual", "" );
	}

	@Test
	void crossJoinValuesRemainIndependentFromSingleRowProfiles() {
		assertThat( new CUBRIDDialect().supportsCrossJoin() ).isTrue();
		assertThat( new AltibaseDialect().supportsCrossJoin() ).isFalse();
		assertThat( new InformixDialect().supportsCrossJoin() ).isFalse();
		assertThat( new SybaseASELegacyDialect().supportsCrossJoin() ).isFalse();
		assertThat( new TimesTenDialect().supportsCrossJoin() ).isFalse();
	}

	private static void assertProfile(Dialect dialect, String tableExpression, String selectOnlyFromClause) {
		final SingleRowTableSupport support = dialect.getSingleRowTableSupport();
		assertThat( support.getTableExpression() )
				.as( dialect.getClass().getSimpleName() + " " + dialect.getVersion() )
				.isEqualTo( tableExpression );
		assertThat( support.getSelectOnlyFromClause() )
				.as( dialect.getClass().getSimpleName() + " " + dialect.getVersion() )
				.isEqualTo( selectOnlyFromClause );
	}
}

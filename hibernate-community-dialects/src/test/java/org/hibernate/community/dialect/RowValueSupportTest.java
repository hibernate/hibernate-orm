/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.DISTINCTNESS_COMPARISON;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.EQUALITY_COMPARISON;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.IN_LIST;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.IN_SUBQUERY;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.ORDERING_COMPARISON;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.QUANTIFIED_COMPARISON;
import static org.hibernate.dialect.sql.ast.spi.RowValueSupport.Feature.ROW_CONSTRUCTOR;

/// Verifies every community Dialect row-value profile, including all
/// version-dependent and asymmetric context combinations.
///
/// @author Steve Ebersole
public class RowValueSupportTest {
	@Test
	void communityDialectsPreserveEveryRowValueFeature() {
		assertProfile( new AltibaseDialect(), EQUALITY_COMPARISON, ORDERING_COMPARISON,
				DISTINCTNESS_COMPARISON, IN_LIST, IN_SUBQUERY );
		assertProfile( new CUBRIDDialect(), EQUALITY_COMPARISON, DISTINCTNESS_COMPARISON, IN_LIST );
		assertProfile( new CockroachLegacyDialect(), ROW_CONSTRUCTOR, EQUALITY_COMPARISON,
				ORDERING_COMPARISON, DISTINCTNESS_COMPARISON, IN_LIST, IN_SUBQUERY );
		assertProfile( new DB2LegacyDialect(), IN_SUBQUERY );
		assertProfile( new HANALegacyDialect(), EQUALITY_COMPARISON, DISTINCTNESS_COMPARISON,
				IN_LIST, IN_SUBQUERY );
		assertProfile( new PostgreSQLLegacyDialect(), ROW_CONSTRUCTOR, EQUALITY_COMPARISON,
				ORDERING_COMPARISON, DISTINCTNESS_COMPARISON, IN_LIST, IN_SUBQUERY,
				QUANTIFIED_COMPARISON );

		assertProfile( new H2LegacyDialect( DatabaseVersion.make( 1, 4, 196 ) ) );
		assertProfile( new H2LegacyDialect( DatabaseVersion.make( 1, 4, 197 ) ),
				EQUALITY_COMPARISON, ORDERING_COMPARISON, IN_LIST, IN_SUBQUERY,
				QUANTIFIED_COMPARISON );
		assertProfile( new H2LegacyDialect( DatabaseVersion.make( 1, 4, 200 ) ),
				EQUALITY_COMPARISON, ORDERING_COMPARISON, DISTINCTNESS_COMPARISON,
				IN_LIST, IN_SUBQUERY, QUANTIFIED_COMPARISON );
		assertProfile( new H2LegacyDialect( DatabaseVersion.make( 2 ) ),
				ROW_CONSTRUCTOR, EQUALITY_COMPARISON, ORDERING_COMPARISON,
				DISTINCTNESS_COMPARISON, IN_LIST, IN_SUBQUERY, QUANTIFIED_COMPARISON );

		assertProfile( new MySQLLegacyDialect( DatabaseVersion.make( 5, 6 ) ),
				EQUALITY_COMPARISON, ORDERING_COMPARISON, DISTINCTNESS_COMPARISON );
		assertProfile( new MySQLLegacyDialect( DatabaseVersion.make( 5, 7 ) ),
				EQUALITY_COMPARISON, ORDERING_COMPARISON, DISTINCTNESS_COMPARISON,
				IN_LIST, IN_SUBQUERY );
		assertProfile( new MariaDBLegacyDialect(),
				EQUALITY_COMPARISON, ORDERING_COMPARISON, DISTINCTNESS_COMPARISON );
		assertProfile( new MariaDBLegacyDialect( DatabaseVersion.make( 5, 7 ) ),
				EQUALITY_COMPARISON, ORDERING_COMPARISON, DISTINCTNESS_COMPARISON,
				IN_LIST, IN_SUBQUERY );
		assertProfile( new TiDBDialect( DatabaseVersion.make( 5, 6 ) ),
				EQUALITY_COMPARISON, ORDERING_COMPARISON, DISTINCTNESS_COMPARISON );
		assertProfile( new TiDBDialect( DatabaseVersion.make( 5, 7 ) ),
				EQUALITY_COMPARISON, ORDERING_COMPARISON, DISTINCTNESS_COMPARISON,
				IN_LIST, IN_SUBQUERY );

		assertProfile( new OracleLegacyDialect( DatabaseVersion.make( 8, 1 ) ) );
		assertProfile( new OracleLegacyDialect( DatabaseVersion.make( 8, 2 ) ), IN_LIST );
		assertProfile( new OracleLegacyDialect( DatabaseVersion.make( 9 ) ), IN_LIST, IN_SUBQUERY );

		for ( Dialect dialect : List.of(
				new CacheDialect(),
				new DerbyDialect(),
				new DerbyLegacyDialect(),
				new FirebirdDialect(),
				new HSQLLegacyDialect(),
				new InformixDialect(),
				new IngresDialect(),
				new InterSystemsIRISDialect(),
				new MaxDBDialect(),
				new MimerSQLDialect(),
				new RDMSOS2200Dialect(),
				new SQLServerLegacyDialect(),
				new SingleStoreDialect(),
				new SybaseLegacyDialect(),
				new TeradataDialect(),
				new TimesTenDialect() ) ) {
			assertProfile( dialect );
		}
	}

	private static void assertProfile(Dialect dialect, RowValueSupport.Feature... features) {
		assertThat( dialect.getRowValueSupport().getFeatures() ).containsExactlyInAnyOrder( features );
	}
}

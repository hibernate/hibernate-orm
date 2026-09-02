/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.spi.TupleCountSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.function.spi.TupleCountSupport.Syntax.ARGUMENT_LIST;
import static org.hibernate.dialect.function.spi.TupleCountSupport.Syntax.PARENTHESIZED_TUPLE;
import static org.hibernate.dialect.function.spi.TupleCountSupport.Syntax.UNSUPPORTED;

/// Verifies every community Dialect tuple-count syntax profile, including
/// inherited profiles and the HSQL legacy version boundary.
///
/// @author Steve Ebersole
public class TupleCountSupportTest {
	@Test
	void communityDialectsPreserveEveryTupleCountForm() {
		for ( Dialect dialect : List.of(
				new CockroachLegacyDialect(),
				new GaussDBDialect(),
				new H2LegacyDialect(),
				new PostgreSQLLegacyDialect(),
				new PostgresPlusLegacyDialect() ) ) {
			assertProfile( dialect, PARENTHESIZED_TUPLE, PARENTHESIZED_TUPLE );
		}

		assertProfile(
				new HSQLLegacyDialect( DatabaseVersion.make( 2, 2, 8 ) ),
				PARENTHESIZED_TUPLE,
				UNSUPPORTED
		);
		assertProfile(
				new HSQLLegacyDialect( DatabaseVersion.make( 2, 2, 9 ) ),
				PARENTHESIZED_TUPLE,
				ARGUMENT_LIST
		);

		for ( Dialect dialect : List.of(
				new AltibaseDialect(),
				new CUBRIDDialect(),
				new DB2LegacyDialect(),
				new DerbyDialect(),
				new DerbyLegacyDialect(),
				new FirebirdDialect(),
				new InformixDialect(),
				new IngresDialect(),
				new InterSystemsIRISDialect(),
				new OracleLegacyDialect(),
				new SQLiteDialect(),
				new TeradataDialect(),
				new TimesTenDialect(),
				new SQLServerLegacyDialect(),
				new SybaseLegacyDialect(),
				new SybaseASELegacyDialect(),
				new SybaseAnywhereDialect() ) ) {
			assertProfile( dialect, UNSUPPORTED, UNSUPPORTED );
		}

		for ( Dialect dialect : List.of(
				new CacheDialect(),
				new HANALegacyDialect(),
				new MySQLLegacyDialect(),
				new MariaDBLegacyDialect(),
				new MaxDBDialect(),
				new MimerSQLDialect(),
				new RDMSOS2200Dialect(),
				new SingleStoreDialect() ) ) {
			assertProfile( dialect, UNSUPPORTED, ARGUMENT_LIST );
		}
	}

	private static void assertProfile(
			Dialect dialect,
			TupleCountSupport.Syntax nonDistinct,
			TupleCountSupport.Syntax distinct) {
		assertThat( dialect.getTupleCountSupport().getNonDistinctSyntax() ).isEqualTo( nonDistinct );
		assertThat( dialect.getTupleCountSupport().getDistinctSyntax() ).isEqualTo( distinct );
	}
}

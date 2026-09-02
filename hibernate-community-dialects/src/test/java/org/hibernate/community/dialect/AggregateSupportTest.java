/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.dialect.DatabaseVersion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies community Dialect composition of maintained aggregate profiles.
///
/// @author Steve Ebersole
/// @since 8.0
public class AggregateSupportTest {
	@Test
	void legacyDialectsReuseMaintainedProfilesWithoutPublicVendorLeaves() {
		for ( var support : List.of(
				new H2LegacyDialect( DatabaseVersion.make( 2, 2, 220 ) ).getAggregateSupport(),
				new HANALegacyDialect( DatabaseVersion.make( 2, 0, 40 ) ).getAggregateSupport(),
				new DB2LegacyDialect( DatabaseVersion.make( 11 ) ).getAggregateSupport(),
				new CockroachLegacyDialect( DatabaseVersion.make( 23 ) ).getAggregateSupport(),
				new MySQLLegacyDialect( DatabaseVersion.make( 8 ) ).getAggregateSupport(),
				new MariaDBLegacyDialect( DatabaseVersion.make( 10, 2 ) ).getAggregateSupport(),
				new OracleLegacyDialect( DatabaseVersion.make( 23 ) ).getAggregateSupport(),
				new PostgreSQLLegacyDialect( DatabaseVersion.make( 17 ) ).getAggregateSupport(),
				new SQLServerLegacyDialect( DatabaseVersion.make( 16 ) ).getAggregateSupport(),
				new SybaseASELegacyDialect( DatabaseVersion.make( 16 ) ).getAggregateSupport(),
				new TiDBDialect().getAggregateSupport() ) ) {
			assertNotNull( support );
			assertTrue( support.getClass().getPackageName().contains( ".aggregate.internal" ) );
		}
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.sql.ast.spi.NullOrdering;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.sql.ast.spi.NullOrderingSupport.Capability.NULLS_FIRST_LAST;

/// Verifies every community null-ordering profile and version boundary.
///
/// @author Steve Ebersole
public class NullOrderingSupportTest {
	@Test
	void fixedCommunityProfilesPreserveProviderValues() {
		assertProfile( new AltibaseDialect(), NullOrdering.LAST, true );
		assertProfile( new CUBRIDDialect(), NullOrdering.SMALLEST, true );
		assertProfile( new HANALegacyDialect(), NullOrdering.SMALLEST, true );
		assertProfile( new HSQLLegacyDialect(), NullOrdering.FIRST, true );
		assertProfile( new CockroachLegacyDialect(), NullOrdering.SMALLEST, false );
		assertProfile( new DB2LegacyDialect(), NullOrdering.GREATEST, false );
		assertProfile( new InterSystemsIRISDialect(), NullOrdering.GREATEST, false );
		assertProfile( new MySQLLegacyDialect(), NullOrdering.SMALLEST, false );
		assertProfile( new SQLServerLegacyDialect(), NullOrdering.SMALLEST, false );
		assertProfile( new SingleStoreDialect(), NullOrdering.SMALLEST, false );
		assertProfile( new SybaseLegacyDialect(), NullOrdering.SMALLEST, false );
	}

	@Test
	void firebirdPreservesIndependentDefaultAndSyntaxBoundaries() {
		assertProfile(
				new FirebirdDialect( DatabaseVersion.make( 1 ) ),
				NullOrdering.LAST,
				false
		);
		assertProfile(
				new FirebirdDialect( DatabaseVersion.make( 1, 5 ) ),
				NullOrdering.LAST,
				true
		);
		assertProfile(
				new FirebirdDialect( DatabaseVersion.make( 2 ) ),
				NullOrdering.SMALLEST,
				true
		);
	}

	@Test
	void explicitSyntaxVersionBoundariesRemainExact() {
		assertProfile(
				new H2LegacyDialect( DatabaseVersion.make( 1, 4, 200 ) ),
				NullOrdering.SMALLEST,
				false
		);
		assertProfile( new H2LegacyDialect( DatabaseVersion.make( 2 ) ), NullOrdering.SMALLEST, true );
		assertProfile(
				new InformixDialect( DatabaseVersion.make( 12, 9 ) ),
				NullOrdering.SMALLEST,
				false
		);
		assertProfile(
				new InformixDialect( DatabaseVersion.make( 12, 10 ) ),
				NullOrdering.SMALLEST,
				true
		);
		assertProfile(
				new SQLiteDialect( DatabaseVersion.make( 3, 2 ) ),
				NullOrdering.SMALLEST,
				false
		);
		assertProfile(
				new SQLiteDialect( DatabaseVersion.make( 3, 3 ) ),
				NullOrdering.SMALLEST,
				true
		);
	}

	private static void assertProfile(Dialect dialect, NullOrdering ordering, boolean explicitSyntax) {
		final NullOrderingSupport support = dialect.getNullOrderingSupport();
		assertThat( support.getDefaultOrdering() )
				.as( dialect.getClass().getSimpleName() + " " + dialect.getVersion() )
				.isEqualTo( ordering );
		assertThat( support.supports( NULLS_FIRST_LAST ) )
				.as( dialect.getClass().getSimpleName() + " " + dialect.getVersion() )
				.isEqualTo( explicitSyntax );
	}
}

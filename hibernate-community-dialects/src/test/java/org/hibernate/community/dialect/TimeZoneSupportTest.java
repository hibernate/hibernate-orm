/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.type.spi.TimeZoneSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies the complete set of community time-zone support supply points.
///
/// @author Steve Ebersole
public class TimeZoneSupportTest {
	@Test
	void communityProfilesPreserveTheirEffectiveValues() {
		assertThat( new CUBRIDDialect().getTimeZoneSupport() ).isEqualTo( TimeZoneSupport.NATIVE );
		assertThat( new CockroachLegacyDialect().getTimeZoneSupport() ).isEqualTo( TimeZoneSupport.NORMALIZE );
		assertThat( new GaussDBDialect().getTimeZoneSupport() ).isEqualTo( TimeZoneSupport.NORMALIZE );
		assertThat( new H2LegacyDialect().getTimeZoneSupport() ).isEqualTo( TimeZoneSupport.NATIVE );
		assertThat( new SybaseAnywhereDialect().getTimeZoneSupport() ).isEqualTo( TimeZoneSupport.NATIVE );
		assertThat( new IngresDialect().getTimeZoneSupport() ).isEqualTo( TimeZoneSupport.NATIVE );
		assertThat( new PostgreSQLLegacyDialect().getTimeZoneSupport() ).isEqualTo( TimeZoneSupport.NORMALIZE );
	}

	@Test
	void versionSensitiveProfilesRemainStable() {
		assertThat( new FirebirdDialect( DatabaseVersion.make( 3 ) ).getTimeZoneSupport() )
				.isEqualTo( TimeZoneSupport.NONE );
		assertThat( new FirebirdDialect( DatabaseVersion.make( 4 ) ).getTimeZoneSupport() )
				.isEqualTo( TimeZoneSupport.NATIVE );
		assertThat( new DB2zLegacyDialect( DatabaseVersion.make( 10 ) ).getTimeZoneSupport() )
				.isEqualTo( TimeZoneSupport.NONE );
		assertThat( new DB2zLegacyDialect( DatabaseVersion.make( 11 ) ).getTimeZoneSupport() )
				.isEqualTo( TimeZoneSupport.NATIVE );
		assertThat( new SQLServerLegacyDialect( DatabaseVersion.make( 9 ) ).getTimeZoneSupport() )
				.isEqualTo( TimeZoneSupport.NONE );
		assertThat( new SQLServerLegacyDialect( DatabaseVersion.make( 10 ) ).getTimeZoneSupport() )
				.isEqualTo( TimeZoneSupport.NATIVE );
		assertThat( new OracleLegacyDialect( DatabaseVersion.make( 8 ) ).getTimeZoneSupport() )
				.isEqualTo( TimeZoneSupport.NONE );
		assertThat( new OracleLegacyDialect( DatabaseVersion.make( 9 ) ).getTimeZoneSupport() )
				.isEqualTo( TimeZoneSupport.NATIVE );
	}
}

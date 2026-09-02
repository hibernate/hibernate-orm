/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;


import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.type.spi.TimeZoneSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies time-zone type profiles and maintained Dialect supply points.
///
/// @author Steve Ebersole
public class TimeZoneSupportTest {
	@Test
	void profilesPreserveTheirEffectiveValues() {
		assertThat( new Dialect( DatabaseVersion.make( 1 ) ) {}.getTimeZoneSupport() )
				.isEqualTo( TimeZoneSupport.NONE );
		assertThat( new H2Dialect().getTimeZoneSupport() ).isEqualTo( TimeZoneSupport.NATIVE );
		assertThat( new PostgreSQLDialect().getTimeZoneSupport() ).isEqualTo( TimeZoneSupport.NORMALIZE );
	}

}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.array.spi.ArraySupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies every community Dialect array profile, including both
/// H2 legacy version branches.
///
/// @author Steve Ebersole
public class ArraySupportTest {
	@Test
	void communityDialectsPreserveEveryArrayDimension() {
		assertProfile(
				new H2LegacyDialect( DatabaseVersion.make( 1, 4, 200 ) ),
				ArraySupport.MultiValuedParameterStrategy.EXPANDED
		);
		assertProfile(
				new H2LegacyDialect( DatabaseVersion.make( 2 ) ),
				ArraySupport.MultiValuedParameterStrategy.EXPANDED,
				ArraySupport.Capability.STANDARD_ARRAY,
				ArraySupport.Capability.ARRAY_CONSTRUCTOR
		);
		for ( Dialect dialect : List.of(
				new HSQLLegacyDialect(),
				new PostgreSQLLegacyDialect(),
				new CockroachLegacyDialect() ) ) {
			assertProfile(
					dialect,
					ArraySupport.MultiValuedParameterStrategy.ARRAY,
					ArraySupport.Capability.STANDARD_ARRAY,
					ArraySupport.Capability.ARRAY_CONSTRUCTOR
			);
		}
		assertProfile(
				new GaussDBDialect(),
				ArraySupport.MultiValuedParameterStrategy.ARRAY,
				ArraySupport.Capability.STANDARD_ARRAY
		);
	}

	private static void assertProfile(
			Dialect dialect,
			ArraySupport.MultiValuedParameterStrategy strategy,
			ArraySupport.Capability... capabilities) {
		assertThat( dialect.getArraySupport().getCapabilities() ).containsExactlyInAnyOrder( capabilities );
		assertThat( dialect.getArraySupport().getMultiValuedParameterStrategy() ).isEqualTo( strategy );
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Release-family selection coverage for migration compatibility.
///
/// @author Steve Ebersole
/// @since 8.0
public class MigrationCompatibilityFamiliesTests {
	@Test
	public void initialMajorHasNoAutomaticBaseline() {
		assertNull( MigrationCompatibilityFamilies.defaultBaseline( "8.0.0-SNAPSHOT" ) );
		assertNull( MigrationCompatibilityFamilies.defaultBaseline( "9.0.0.Final" ) );
	}

	@Test
	public void initialMinorUsesPreviousMinor() {
		assertEquals( "8.0", MigrationCompatibilityFamilies.defaultBaseline( "8.1.0-SNAPSHOT" ) );
		assertEquals( "8.1", MigrationCompatibilityFamilies.defaultBaseline( "8.2.0.CR1" ) );
	}

	@Test
	public void maintenanceUsesCurrentFamily() {
		assertEquals( "8.1", MigrationCompatibilityFamilies.defaultBaseline( "8.1.1-SNAPSHOT" ) );
		assertEquals( "8.1", MigrationCompatibilityFamilies.defaultBaseline( "8.1.12.Final" ) );
	}

	@Test
	public void malformedVersionsAndFamiliesAreRejected() {
		assertThrows(
				IllegalArgumentException.class,
				() -> MigrationCompatibilityFamilies.defaultBaseline( "8.1-SNAPSHOT" )
		);
		assertThrows(
				IllegalArgumentException.class,
				() -> MigrationCompatibilityFamilies.requireFamily( "8.1.0" )
		);
	}
}

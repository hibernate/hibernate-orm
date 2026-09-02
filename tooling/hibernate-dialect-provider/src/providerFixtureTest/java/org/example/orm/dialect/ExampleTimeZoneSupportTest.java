/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.type.spi.TimeZoneSupport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies time-zone type support supplied by the standalone provider.
///
/// @author Steve Ebersole
public class ExampleTimeZoneSupportTest {
	@Test
	void suppliesAProviderOwnedNondefaultProfile() {
		assertEquals( TimeZoneSupport.NORMALIZE, new ExampleDialect().getTimeZoneSupport() );
	}
}

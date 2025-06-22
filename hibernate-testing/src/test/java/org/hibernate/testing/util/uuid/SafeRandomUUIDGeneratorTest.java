/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.util.uuid;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Jan Schatteman
 */
public class SafeRandomUUIDGeneratorTest {

	@Test
	public void testSafeUUIDGenerator() {
		String nonZeroEnd = "53886a8a-7082-4879-b430-25cb94415be8";
		String zeroEnd = "53886a8a-7082-4879-b430-25cb94415b00";
		assertTrue( SafeRandomUUIDGenerator.isSafeUUID( UUID.fromString( nonZeroEnd )) );
		assertFalse( SafeRandomUUIDGenerator.isSafeUUID( UUID.fromString( zeroEnd )) );
	}
}

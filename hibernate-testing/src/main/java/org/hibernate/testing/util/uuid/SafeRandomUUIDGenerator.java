/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.util.uuid;

import java.util.UUID;

/**
 * @author Jan Schatteman
 */
public class SafeRandomUUIDGenerator {

	/**
	 * @return A random UUID that is guaranteed not to end with a trailing 0 byte
	 */
	public static UUID safeRandomUUID() {
		UUID uuid = UUID.randomUUID();
		if ( !isSafeUUID(uuid) ) {
			uuid = safeRandomUUID();
		}
		return uuid;
	}

	/**
	 * @return The String representation of a random UUID that is guaranteed not to end with a trailing 0 byte
	 */
	public static String safeRandomUUIDAsString() {
		return safeRandomUUID().toString();
	}

	/**
	 * @param uuid The UUID to be tested
	 * @return true if the UUID doesn't end with a trailing 0 byte, false otherwise
	 */
	public static boolean isSafeUUID(UUID uuid) {
		if ( uuid == null ) {
			throw new RuntimeException("The UUID cannot be tested if it's null!");
		}
		return ( (uuid.getLeastSignificantBits() & 0xFF) != 0 );
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.type.internal.StandardEnumSupport;

import static org.hibernate.SPI.Role.USE;

/// Supplies immutable stock enum and finite-domain profiles.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public final class EnumSupports {
	private static final EnumSupport STANDARD = StandardEnumSupport.standard();
	private static final EnumSupport INLINE = StandardEnumSupport.inline();
	private static final EnumSupport H2 = StandardEnumSupport.h2();
	private static final EnumSupport POSTGRESQL = StandardEnumSupport.postgresql();

	private EnumSupports() {
	}

	/// Return the profile with standard checks and no database enum type.
	public static EnumSupport standard() {
		return STANDARD;
	}

	/// Return the inline `enum (...)` declaration profile.
	public static EnumSupport inline() {
		return INLINE;
	}

	/// Return the H2 inline and named-domain profile.
	public static EnumSupport h2() {
		return H2;
	}

	/// Return the PostgreSQL named-type lifecycle profile.
	public static EnumSupport postgresql() {
		return POSTGRESQL;
	}

	/// Return the Oracle version-aware named-domain profile.
	public static EnumSupport oracle(DatabaseVersion version) {
		return StandardEnumSupport.oracle( version );
	}
}

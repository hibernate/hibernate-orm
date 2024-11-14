/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

/**
 * An enumeration of truth values.
 *
 * @implNote Sure, this could be handled with {@code Boolean}, but
 *           that option is vulnerable to unwanted auto-unboxing
 *           and {@link NullPointerException}s.
 *
 * @author Steve Ebersole
 */
public enum TruthValue {
	TRUE,
	FALSE,
	UNKNOWN;

	public static TruthValue of(boolean bool) {
		return bool ? TRUE : FALSE;
	}

	public boolean toBoolean(boolean defaultValue) {
		return switch ( this ) {
			case TRUE -> true;
			case FALSE -> false;
			default -> defaultValue;
		};
	}

	/**
	 * @deprecated No longer used
	 */
	@Deprecated(since = "7", forRemoval = true)
	public static boolean toBoolean(TruthValue value, boolean defaultValue) {
		return value != null ? value.toBoolean( defaultValue ) : defaultValue;
	}
}

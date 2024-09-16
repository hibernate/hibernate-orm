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

	public boolean toBoolean(boolean defaultValue) {
		switch (this) {
			case TRUE:
				return true;
			case FALSE:
				return false;
			default:
				return defaultValue;
		}
	}

	public static boolean toBoolean(TruthValue value, boolean defaultValue) {
		return value != null ? value.toBoolean( defaultValue ) : defaultValue;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

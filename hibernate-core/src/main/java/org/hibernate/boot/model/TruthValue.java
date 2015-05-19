/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

/**
 * An enumeration of truth values.
 * <p/>
 * Yes this *could* be handled with Boolean, but then you run into potential
 * problems with unwanted auto-unboxing.
 *
 * @author Steve Ebersole
 */
public enum TruthValue {
	TRUE,
	FALSE,
	UNKNOWN;

	@SuppressWarnings("SimplifiableIfStatement")
	public boolean toBoolean(boolean defaultValue) {
		if ( this == TRUE ) {
			return true;
		}
		else if ( this == FALSE ) {
			return false;
		}
		else {
			return defaultValue;
		}
	}

	public static boolean toBoolean(TruthValue value, boolean defaultValue) {
		return value != null ? value.toBoolean( defaultValue ) : defaultValue;
	}
}


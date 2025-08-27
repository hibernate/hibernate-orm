/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

import java.util.function.Function;

/**
 * How to handle features in the transformed `hbm.xml` which are not supported
 * in the `mapping.xml` XSD
 *
 * @author Steve Ebersole
 */
public enum UnsupportedFeatureHandling {
	/**
	 * Throw an exception.
	 */
	ERROR,
	/**
	 * Similar to {@link #IGNORE} except that we log a warning
	 */
	WARN,
	/**
	 * Simply ignore the feature.  Logs a debug message
	 */
	IGNORE,
	/**
	 * Pick the closest mapping, if possible.  Falls back to {@link #IGNORE} if there is no close match
	 */
	PICK;

	public static UnsupportedFeatureHandling fromSetting(Object value) {
		return fromSetting( value, (v) -> null );
	}

	public static UnsupportedFeatureHandling fromSetting(Object value, UnsupportedFeatureHandling defaultValue) {
		return fromSetting( value, (v) -> defaultValue );
	}

	public static UnsupportedFeatureHandling fromSetting(Object value, Function<Object, UnsupportedFeatureHandling> defaultValueSupplier) {
		if ( value != null ) {
			if ( value instanceof UnsupportedFeatureHandling unsupportedFeatureHandling ) {
				return unsupportedFeatureHandling;
			}
			else if ( ERROR.name().equalsIgnoreCase( value.toString() ) ) {
				return ERROR;
			}
			else if ( IGNORE.name().equalsIgnoreCase( value.toString() ) ) {
				return IGNORE;
			}
			else if ( PICK.name().equalsIgnoreCase( value.toString() ) ) {
				return PICK;
			}
		}

		return defaultValueSupplier.apply( value );
	}
}

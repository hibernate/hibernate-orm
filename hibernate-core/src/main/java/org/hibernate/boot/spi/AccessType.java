/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.AssertionFailure;

/**
 * Enumerates various access strategies for accessing entity values.
 *
 * @author Hardy Ferentschik
 */
public enum AccessType {
	/**
	 * Default access strategy is property
	 */
	DEFAULT,

	/**
	 * Access to value via property
	 */
	PROPERTY,

	/**
	 * Access to value via field
	 */
	FIELD,

	/**
	 * Access to value via record component accessor
	 */
	RECORD;

	/**
	 * Retrieves the external name for this access type
	 *
	 * @return The external name
	 */
	public String getType() {
		switch (this) {
			case DEFAULT:
			case PROPERTY:
				return "property";
			case FIELD:
				return "field";
			case RECORD:
				return "record";
			default:
				throw new AssertionFailure("Unknown AccessType");
		}
	}

	/**
	 * Resolve an externalized name to the {@code AccessType} value it names.
	 *
	 * @param externalName The external name
	 *
	 * @return The matching {@code AccessType};
	 *         {@link #DEFAULT} is returned rather than {@code null}
	 */
	public static AccessType getAccessStrategy(String externalName) {
		if ( externalName == null ) {
			return DEFAULT;
		}
		for ( AccessType value : values() ) {
			if ( value.getType().equals( externalName ) ) {
				return value;
			}
		}
		return DEFAULT; // because sometimes we get passed magic values like "unsupported"
	}

	/**
	 * Convert the JPA access type to the corresponding {@link AccessType} value.
	 *
	 * @param type The JPA access type
	 *
	 * @return The Hibernate {@link AccessType}
	 */
	public static AccessType getAccessStrategy(jakarta.persistence.AccessType type) {
		if ( type == null ) {
			return DEFAULT;
		}
		switch ( type ) {
			case FIELD:
				return FIELD;
			case PROPERTY:
				return PROPERTY;
			default:
				throw new AssertionFailure( "unrecognized AccessType" );
		}
	}
}

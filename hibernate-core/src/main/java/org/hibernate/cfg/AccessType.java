/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cfg;

/**
 * Enum defining different access strategies for accessing entity values.
 *
 * @author Hardy Ferentschik
 */
public enum AccessType {
	/**
	 * Default access strategy is property
	 */
	DEFAULT( "property" ),

	/**
	 * Access to value via property
	 */
	PROPERTY( "property" ),

	/**
	 * Access to value via field
	 */
	FIELD( "field" );

	private final String accessType;

	private AccessType(String type) {
		this.accessType = type;
	}

	/**
	 * Retrieves the external name for this access type
	 *
	 * @return The external name
	 */
	public String getType() {
		return accessType;
	}

	/**
	 * Resolve an externalized name to the AccessType enum value it names.
	 *
	 * @param externalName The external name
	 *
	 * @return The matching AccessType; {@link #DEFAULT} is returned rather than {@code null}
	 */
	public static AccessType getAccessStrategy(String externalName) {
		if ( externalName == null ) {
			return DEFAULT;
		}
		else if ( FIELD.getType().equals( externalName ) ) {
			return FIELD;
		}
		else if ( PROPERTY.getType().equals( externalName ) ) {
			return PROPERTY;
		}
		else {
			// TODO historically if the externalName string could not be matched default access was used. Maybe this should be an exception though!?
			return DEFAULT;
		}
	}

	/**
	 * Convert the JPA access type enum to the corresponding AccessType enum value.
	 *
	 * @param type The JPA enum value
	 *
	 * @return The Hibernate AccessType
	 */
	public static AccessType getAccessStrategy(javax.persistence.AccessType type) {
		if ( javax.persistence.AccessType.PROPERTY.equals( type ) ) {
			return PROPERTY;
		}
		else if ( javax.persistence.AccessType.FIELD.equals( type ) ) {
			return FIELD;
		}
		else {
			return DEFAULT;
		}
	}
}

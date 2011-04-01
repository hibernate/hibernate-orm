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

	AccessType(String type) {
		this.accessType = type;
	}

	public String getType() {
		return accessType;
	}

	public static AccessType getAccessStrategy(String type) {
		if ( type == null ) {
			return DEFAULT;
		}
		else if ( FIELD.getType().equals( type ) ) {
			return FIELD;
		}
		else if ( PROPERTY.getType().equals( type ) ) {
			return PROPERTY;
		}
		else {
			// TODO historically if the type string could not be matched default access was used. Maybe this should be an exception though!?
			return DEFAULT;
		}
	}

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

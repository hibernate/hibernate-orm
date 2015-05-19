/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

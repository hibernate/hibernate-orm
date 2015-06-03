/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi.access;

import java.util.Locale;

/**
 * The types of access strategies available.
 *
 * @author Steve Ebersole
 */
public enum AccessType {
	/**
	 * Read-only access.  Data may be added and removed, but not mutated.
	 */
	READ_ONLY( "read-only" ),
	/**
	 * Read and write access (strict).  Data may be added, removed and mutated.
	 */
	READ_WRITE( "read-write" ),
	/**
	 * Read and write access (non-strict).  Data may be added, removed and mutated.  The non-strictness comes from
	 * the fact that locks are not maintained as tightly as in {@link #READ_WRITE}, which leads to better throughput
	 * but may also lead to inconsistencies.
	 */
	NONSTRICT_READ_WRITE( "nonstrict-read-write" ),
	/**
	 * A read and write strategy where isolation/locking is maintained in conjunction with a JTA transaction.
	 */
	TRANSACTIONAL( "transactional" );

	private final String externalName;

	private AccessType(String externalName) {
		this.externalName = externalName;
	}

	/**
	 * Get the corresponding externalized name for this value.
	 *
	 * @return The corresponding externalized name.
	 */
	public String getExternalName() {
		return externalName;
	}

	@Override
	public String toString() {
		return "AccessType[" + externalName + "]";
	}

	/**
	 * Resolve an AccessType from its external name.
	 *
	 * @param externalName The external representation to resolve
	 *
	 * @return The access type.
	 *
	 * @throws UnknownAccessTypeException If the externalName was not recognized.
	 *
	 * @see #getExternalName()
	 */
	public static AccessType fromExternalName(String externalName) {
		if ( externalName == null ) {
			return null;
		}
		for ( AccessType accessType : AccessType.values() ) {
			if ( accessType.getExternalName().equals( externalName ) ) {
				return accessType;
			}
		}
		// Check to see if making upper-case matches an enum name.
		try {
			return AccessType.valueOf( externalName.toUpperCase( Locale.ROOT) );
		}
		catch ( IllegalArgumentException e ) {
			throw new UnknownAccessTypeException( externalName );
		}
	}
}

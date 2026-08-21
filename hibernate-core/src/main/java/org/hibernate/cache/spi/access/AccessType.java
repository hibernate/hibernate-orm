/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.access;

import java.util.Locale;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Enumerates the policies for managing concurrent access to the shared
 * second-level cache.
 *
 * @apiNote This enumeration is isomorphic to
 *          {@link org.hibernate.annotations.CacheConcurrencyStrategy}.
 *          We don't really need both, but one is part of this SPI,
 *          and one forms part of the API of the annotations package.
 *          In the future, it would be nice to replace them both with
 *          a new {@code org.hibernate.CacheConcurrencyPolicy} enum.
 *
 * @author Steve Ebersole
 */
public enum AccessType {
	/**
	 * Read-only access. Data may be added and removed, but not mutated.
	 */
	READ_ONLY,
	/**
	 * Read and write access. Data may be added, removed and mutated.
	 * A "soft" lock on the cached item is used to manage concurrent
	 * access during mutation.
	 */
	READ_WRITE,
	/**
	 * Read and write access. Data may be added, removed and mutated.
	 * The cached item is invalidated before and after transaction
	 * completion to manage concurrent access during mutation. This
	 * strategy is more vulnerable to inconsistencies than
	 * {@link #READ_WRITE}, but may allow higher throughput.
	 */
	NONSTRICT_READ_WRITE,
	/**
	 * Read and write access. Data may be added, removed and mutated.
	 * Some sort of hard lock is maintained in conjunction with a
	 * JTA transaction.
	 */
	TRANSACTIONAL;

	/**
	 * Get the external name of this value.
	 *
	 * @return The corresponding externalized name.
	 */
	public String getExternalName() {
		return super.toString().toLowerCase(Locale.ROOT).replace('_','-');
	}

	@Override
	public String toString() {
		return "AccessType[" + getExternalName() + "]";
	}

	/**
	 * Resolve an {@link AccessType} from its external name.
	 *
	 * @param externalName The external representation to resolve
	 * @return The {@link AccessType} represented by the given external name
	 * @throws UnknownAccessTypeException if the external name was not recognized
	 *
	 * @see #getExternalName()
	 */
	public static AccessType fromExternalName(@Nullable String externalName) {
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
			return AccessType.valueOf( externalName.toUpperCase( Locale.ROOT ) );
		}
		catch ( IllegalArgumentException e ) {
			throw new UnknownAccessTypeException( externalName );
		}
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.cache.spi.access.AccessType;

/**
 * Cache concurrency strategy.
 *
 * @author Emmanuel Bernard
 */
public enum CacheConcurrencyStrategy {
	/**
	 * Indicates no concurrency strategy should be applied.
	 */
	NONE( null ),
	/**
	 * Indicates that read-only strategy should be applied.
	 *
	 * @see AccessType#READ_ONLY
	 */
	READ_ONLY( AccessType.READ_ONLY ),
	/**
	 * Indicates that the non-strict read-write strategy should be applied.
	 *
	 * @see AccessType#NONSTRICT_READ_WRITE
	 */
	NONSTRICT_READ_WRITE( AccessType.NONSTRICT_READ_WRITE ),
	/**
	 * Indicates that the read-write strategy should be applied.
	 *
	 * @see AccessType#READ_WRITE
	 */
	READ_WRITE( AccessType.READ_WRITE ),
	/**
	 * Indicates that the transaction strategy should be applied.
	 *
	 * @see AccessType#TRANSACTIONAL
	 */
	TRANSACTIONAL( AccessType.TRANSACTIONAL );

	private final AccessType accessType;

	private CacheConcurrencyStrategy(AccessType accessType) {
		this.accessType = accessType;
	}

	/**
	 * Get the AccessType corresponding to this concurrency strategy.
	 *
	 * @return The corresponding concurrency strategy.  Note that this will return {@code null} for
	 * {@link #NONE}
	 */
	public AccessType toAccessType() {
		return accessType;
	}

	/**
	 * Conversion from {@link AccessType} to {@link CacheConcurrencyStrategy}.
	 *
	 * @param accessType The access type to convert
	 *
	 * @return The corresponding enum value.  {@link #NONE} is returned by default if unable to
	 * recognize {@code accessType} or if {@code accessType} is {@code null}.
	 */
	public static CacheConcurrencyStrategy fromAccessType(AccessType accessType) {
		if ( null == accessType ) {
			return NONE;
		}
		
		switch ( accessType ) {
			case READ_ONLY: {
				return READ_ONLY;
			}
			case READ_WRITE: {
				return READ_WRITE;
			}
			case NONSTRICT_READ_WRITE: {
				return NONSTRICT_READ_WRITE;
			}
			case TRANSACTIONAL: {
				return TRANSACTIONAL;
			}
			default: {
				return NONE;
			}
		}
	}

	/**
	 * Parse an external representation of a CacheConcurrencyStrategy value.
	 *
	 * @param name The external representation
	 *
	 * @return The corresponding enum value, or {@code null} if not match was found.
	 */
	public static CacheConcurrencyStrategy parse(String name) {
		if ( READ_ONLY.isMatch( name ) ) {
			return READ_ONLY;
		}
		else if ( READ_WRITE.isMatch( name ) ) {
			return READ_WRITE;
		}
		else if ( NONSTRICT_READ_WRITE.isMatch( name ) ) {
			return NONSTRICT_READ_WRITE;
		}
		else if ( TRANSACTIONAL.isMatch( name ) ) {
			return TRANSACTIONAL;
		}
		else if ( NONE.isMatch( name ) ) {
			return NONE;
		}
		else {
			return null;
		}
	}

	private boolean isMatch(String name) {
		return ( accessType != null && accessType.getExternalName().equalsIgnoreCase( name ) )
				|| name().equalsIgnoreCase( name );
	}
}

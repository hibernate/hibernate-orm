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
package org.hibernate.annotations;

import org.hibernate.cache.spi.access.AccessType;

/**
 * Cache concurrency strategy
 *
 * @author Emmanuel Bernard
 */
public enum CacheConcurrencyStrategy {
	NONE( null ),
	READ_ONLY( AccessType.READ_ONLY ),
	NONSTRICT_READ_WRITE( AccessType.NONSTRICT_READ_WRITE ),
	READ_WRITE( AccessType.READ_WRITE ),
	TRANSACTIONAL( AccessType.TRANSACTIONAL );

	private final AccessType accessType;

	private CacheConcurrencyStrategy(AccessType accessType) {
		this.accessType = accessType;
	}

	private boolean isMatch(String name) {
		return ( accessType != null && accessType.getExternalName().equalsIgnoreCase( name ) )
				|| name().equalsIgnoreCase( name );
	}

	public static CacheConcurrencyStrategy fromAccessType(AccessType accessType) {
		if (null == accessType) {
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

	public AccessType toAccessType() {
		return accessType;
	}
}

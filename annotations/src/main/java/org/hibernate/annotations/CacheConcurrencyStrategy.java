/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

import org.hibernate.cache.access.AccessType;

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

	public static CacheConcurrencyStrategy fromAccessType(AccessType accessType) {
		final String name = accessType == null ? null : accessType.getName();
		if ( AccessType.READ_ONLY.getName().equals( name ) ) {
			return READ_ONLY;
		}
		else if ( AccessType.READ_WRITE.getName().equals( name ) ) {
			return READ_WRITE;
		}
		else if ( AccessType.NONSTRICT_READ_WRITE.getName().equals( name ) ) {
			return NONSTRICT_READ_WRITE;
		}
		else if ( AccessType.TRANSACTIONAL.getName().equals( name ) ) {
			return TRANSACTIONAL;
		}
		else {
			return NONE;
		}
	}

	public static CacheConcurrencyStrategy parse(String name) {
		if ( READ_ONLY.accessType.getName().equalsIgnoreCase( name ) ) {
			return READ_ONLY;
		}
		else if ( READ_WRITE.accessType.getName().equalsIgnoreCase( name ) ) {
			return READ_WRITE;
		}
		else if ( NONSTRICT_READ_WRITE.accessType.getName().equalsIgnoreCase( name ) ) {
			return NONSTRICT_READ_WRITE;
		}
		else if ( TRANSACTIONAL.accessType.getName().equalsIgnoreCase( name ) ) {
			return TRANSACTIONAL;
		}
		else if ( "none".equalsIgnoreCase( name ) ) {
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

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.spi.access;

/**
 * The types of access strategies available.
 *
 * @author Steve Ebersole
 */
public enum AccessType {
	READ_ONLY( "read-only" ),
	READ_WRITE( "read-write" ),
	NONSTRICT_READ_WRITE( "nonstrict-read-write" ),
	TRANSACTIONAL( "transactional" );

	private final String externalName;

	private AccessType(String externalName) {
		this.externalName = externalName;
	}

	public String getExternalName() {
		return externalName;
	}

	public String toString() {
		return "AccessType[" + externalName + "]";
	}

	public static AccessType fromExternalName(String externalName) {
		if ( externalName == null ) {
			return null;
		}
		for ( AccessType accessType : AccessType.values() ) {
			if ( accessType.getExternalName().equals( externalName ) ) {
				return accessType;
			}
		}
		throw new UnknownAccessTypeException( externalName );
	}
}

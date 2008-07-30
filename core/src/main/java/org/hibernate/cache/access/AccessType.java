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
 *
 */
package org.hibernate.cache.access;

import java.io.Serializable;

/**
 * The types of access strategies available.
 *
 * @author Steve Ebersole
 */
public class AccessType implements Serializable {
	public static final AccessType READ_ONLY = new AccessType( "read-only" );
	public static final AccessType READ_WRITE = new AccessType( "read-write" );
	public static final AccessType NONSTRICT_READ_WRITE = new AccessType( "nonstrict-read-write" );
	public static final AccessType TRANSACTIONAL = new AccessType( "transactional" );

	private final String name;

	private AccessType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return "AccessType[" + name + "]";
	}

	private static AccessType resolve(String name) {
		if ( READ_ONLY.name.equals( name ) ) {
			return READ_ONLY;
		}
		else if ( READ_WRITE.name.equals( name ) ) {
			return READ_WRITE;
		}
		else if ( NONSTRICT_READ_WRITE.name.equals( name ) ) {
			return NONSTRICT_READ_WRITE;
		}
		else if ( TRANSACTIONAL.name.equals( name ) ) {
			return TRANSACTIONAL;
		}
		else {
			return null;
		}
	}

	public static AccessType parse(String name) {
		return resolve( name );
	}

	private Object readResolve() {
		return resolve( name );
	}

	public static String getValidUsageString() {
		return "cache usage attribute should be " + READ_ONLY.name +
				", " + READ_WRITE.name +
				", " + NONSTRICT_READ_WRITE.name +
				", or " + TRANSACTIONAL.name;
	}
}

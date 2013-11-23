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
package org.hibernate.jpa.internal.util;

import javax.persistence.LockModeType;

import org.hibernate.LockMode;
import org.hibernate.internal.util.LockModeConverter;

/**
 * Helper to deal with {@link LockModeType} <-> {@link LockMode} conversions.
 *
 * @author Steve Ebersole
 */
public final class LockModeTypeHelper {
	private LockModeTypeHelper() {
	}

	public static LockModeType getLockModeType(LockMode lockMode) {
		return LockModeConverter.convertToLockModeType( lockMode );
	}

	public static LockMode getLockMode(LockModeType lockModeType) {
		return LockModeConverter.convertToLockMode( lockModeType );
	}

	public static LockMode interpretLockMode(Object value) {
		if ( value == null ) {
			return LockMode.NONE;
		}
		if ( LockMode.class.isInstance( value ) ) {
			return (LockMode) value;
		}
		else if ( LockModeType.class.isInstance( value ) ) {
			return getLockMode( (LockModeType) value );
		}
		else if ( String.class.isInstance( value ) ) {
			// first try LockMode name
			LockMode lockMode = LockMode.valueOf( (String) value );
			if ( lockMode == null ) {
				try {
					lockMode = getLockMode( LockModeType.valueOf( (String) value ) );
				}
				catch ( Exception ignore ) {
				}
			}
			if ( lockMode != null ) {
				return lockMode;
			}
		}

		throw new IllegalArgumentException( "Unknown lock mode source : " + value );
	}

}

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
package org.hibernate.ejb.util;

import javax.persistence.LockModeType;

import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;

/**
 * Helper to deal with {@link LockModeType} <-> {@link LockMode} conversions.
 *
 * @author Steve Ebersole
 */
public class LockModeTypeHelper {
	public static LockModeType getLockModeType(LockMode lockMode) {
		if ( lockMode == LockMode.NONE ) {
			return LockModeType.NONE;
		}
		else if ( lockMode == LockMode.OPTIMISTIC || lockMode == LockMode.READ ) {
			return LockModeType.OPTIMISTIC;
		}
		else if ( lockMode == LockMode.OPTIMISTIC_FORCE_INCREMENT || lockMode == LockMode.WRITE ) {
			return LockModeType.OPTIMISTIC_FORCE_INCREMENT;
		}
		else if ( lockMode == LockMode.PESSIMISTIC_READ ) {
			return LockModeType.PESSIMISTIC_READ;
		}
		else if ( lockMode == LockMode.PESSIMISTIC_WRITE
				|| lockMode == LockMode.UPGRADE
				|| lockMode == LockMode.UPGRADE_NOWAIT ) {
			return LockModeType.PESSIMISTIC_WRITE;
		}
		else if ( lockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT
				|| lockMode == LockMode.FORCE ) {
			return LockModeType.PESSIMISTIC_FORCE_INCREMENT;
		}
		throw new AssertionFailure( "unhandled lock mode " + lockMode );
	}


	public static LockMode getLockMode(LockModeType lockMode) {
		switch ( lockMode ) {
			case READ:
			case OPTIMISTIC: {
				return LockMode.OPTIMISTIC;
			}
			case OPTIMISTIC_FORCE_INCREMENT:
			case WRITE: {
				return LockMode.OPTIMISTIC_FORCE_INCREMENT;
			}
			case PESSIMISTIC_READ: {
				return LockMode.PESSIMISTIC_READ;
			}
			case PESSIMISTIC_WRITE: {
				return LockMode.PESSIMISTIC_WRITE;
			}
			case PESSIMISTIC_FORCE_INCREMENT: {
				return LockMode.PESSIMISTIC_FORCE_INCREMENT;
			}
			case NONE: {
				return LockMode.NONE;
			}
			default: {
				throw new AssertionFailure( "Unknown LockModeType: " + lockMode );
			}
		}
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
			LockMode lockMode = LockMode.parse( (String) value );
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

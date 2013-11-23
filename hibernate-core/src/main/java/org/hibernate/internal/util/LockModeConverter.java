/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.internal.util;

import javax.persistence.LockModeType;

import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;

/**
 * Helper to deal with conversions (both directions) between {@link org.hibernate.LockMode} and
 * {@link javax.persistence.LockModeType}.
 *
 * @author Steve Ebersole
 */
public final class LockModeConverter {
	private LockModeConverter() {
	}

	/**
	 * Convert from the Hibernate specific LockMode to the JPA defined LockModeType.
	 *
	 * @param lockMode The Hibernate LockMode.
	 *
	 * @return The JPA LockModeType
	 */
	public static LockModeType convertToLockModeType(LockMode lockMode) {
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
				|| lockMode == LockMode.UPGRADE_NOWAIT
                || lockMode == LockMode.UPGRADE_SKIPLOCKED) {
			return LockModeType.PESSIMISTIC_WRITE;
		}
		else if ( lockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT
				|| lockMode == LockMode.FORCE ) {
			return LockModeType.PESSIMISTIC_FORCE_INCREMENT;
		}
		throw new AssertionFailure( "unhandled lock mode " + lockMode );
	}


	/**
	 * Convert from JPA defined LockModeType to Hibernate specific LockMode.
	 *
	 * @param lockMode The JPA LockModeType
	 *
	 * @return The Hibernate LockMode.
	 */
	public static LockMode convertToLockMode(LockModeType lockMode) {
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
}

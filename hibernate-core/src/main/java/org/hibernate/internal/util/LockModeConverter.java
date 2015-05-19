/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

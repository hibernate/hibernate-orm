/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine;

/**
 * Describes how an entity should be optimistically locked.
 *
 * @author Steve Ebersole
 */
public enum OptimisticLockStyle {
	/**
	 * no optimistic locking
	 */
	NONE( -1 ),
	/**
	 * use a dedicated version column
	 */
	VERSION( 0 ),
	/**
	 * dirty columns are compared
	 */
	DIRTY( 1 ),
	/**
	 * all columns are compared
	 */
	ALL( 2 );

	private final int oldCode;

	private OptimisticLockStyle(int oldCode) {
		this.oldCode = oldCode;
	}

	public int getOldCode() {
		return oldCode;
	}

	/**
	 * Given an old code (one of the int constants from Cascade), interpret it as one of the new enums.
	 *
	 * @param oldCode The old int constant code
	 *
	 * @return The interpreted enum value
	 *
	 * @throws IllegalArgumentException If the code did not match any legacy constant.
	 */
	public static OptimisticLockStyle interpretOldCode(int oldCode) {
		switch ( oldCode ) {
			case -1: {
				return NONE;
			}
			case 0: {
				return VERSION;
			}
			case 1: {
				return DIRTY;
			}
			case 2: {
				return ALL;
			}
			default: {
				throw new IllegalArgumentException( "Illegal legacy optimistic lock style code :" + oldCode );
			}
		}
	}
}

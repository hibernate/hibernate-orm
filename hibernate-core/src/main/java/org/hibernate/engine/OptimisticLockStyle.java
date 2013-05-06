/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

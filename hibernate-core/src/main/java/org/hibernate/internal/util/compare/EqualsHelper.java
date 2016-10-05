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
package org.hibernate.internal.util.compare;

import java.util.Arrays;

/**
 * @author Gavin King
 */
public final class EqualsHelper {

	public static boolean equals(final Object x, final Object y) {
		if (x == y || (x != null && y != null && x.equals(y))) {
			return true;
		}
		if (x == null && y != null) {
			return false;
		}
		if (x != null && y == null) {
			return false;
		}
		Class<?> xClass = x.getClass();
		Class<?> yClass = y.getClass();
		if (xClass.isArray()) {
			if (xClass != yClass.getClass()) {
				// different dimensions, ex: int[][] to a int[]
				return false;
			}
			if (x instanceof long[]) {
				return Arrays.equals((long[]) x, (long[]) y);
			} else if (x instanceof int[]) {
				return Arrays.equals((int[]) x, (int[]) y);
			} else if (x instanceof short[]) {
				return Arrays.equals((short[]) x, (short[]) y);
			} else if (x instanceof char[]) {
				return Arrays.equals((char[]) x, (char[]) y);
			} else if (x instanceof byte[]) {
				return Arrays.equals((byte[]) x, (byte[]) y);
			} else if (x instanceof double[]) {
				return Arrays.equals((double[]) x, (double[]) y);
			} else if (x instanceof float[]) {
				return Arrays.equals((float[]) x, (float[]) y);
			} else if (x instanceof boolean[]) {
				return Arrays.equals((boolean[]) x, (boolean[]) y);
			} else {
				return Arrays.deepEquals((Object[]) x, (Object[]) y);
			}
		}
		return false;
	}
	
	private EqualsHelper() {}

}

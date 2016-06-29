/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.compare;

import java.util.Comparator;

/**
 * @author Gail Badner
 */
public final class RowVersionComparator implements Comparator<byte[]> {
	public static RowVersionComparator INSTANCE = new RowVersionComparator();

	private RowVersionComparator() {
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public int compare(byte[] o1, byte[] o2) {
		final int lengthToCheck = Math.min( o1.length, o2.length );

		for ( int i = 0 ; i < lengthToCheck ; i++ ) {
			// must do an unsigned int comparison
			// Byte#toUnsignedInt is available starting in jdk8,
			// so directly convert to unsigned int before comparing
			final int comparison = ComparableComparator.INSTANCE.compare(
					( (int) o1[i] ) & 0xff,
					( (int) o2[i] ) & 0xff
			);
			if ( comparison != 0 ) {
				return comparison;
			}
		}
		return o1.length - o2.length;
	}
}

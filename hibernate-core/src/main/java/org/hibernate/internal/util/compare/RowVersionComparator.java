/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.compare;

import java.util.Comparator;

/**
 * @author Gail Badner
 */
public final class RowVersionComparator implements Comparator<byte[]> {

	public static final RowVersionComparator INSTANCE = new RowVersionComparator();

	private RowVersionComparator() {
	}

	@Override
	public int compare(byte[] o1, byte[] o2) {
		final int lengthToCheck = Math.min( o1.length, o2.length );

		for ( int i = 0 ; i < lengthToCheck ; i++ ) {
			// must do an unsigned int comparison
			final int comparison = ComparableComparator.<Integer>instance().compare(
						Byte.toUnsignedInt( o1[i] ),
						Byte.toUnsignedInt( o2[i] )
			);
			if ( comparison != 0 ) {
				return comparison;
			}
		}
		return o1.length - o2.length;
	}
}

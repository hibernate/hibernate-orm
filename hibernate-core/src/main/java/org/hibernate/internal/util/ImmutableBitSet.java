/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

import java.util.Arrays;
import java.util.BitSet;

/**
 * An immutable variant of the {@link BitSet} class with some additional operations.
 */
public class ImmutableBitSet {

	public static final ImmutableBitSet EMPTY = new ImmutableBitSet( new long[0] );

	private static final int ADDRESS_BITS_PER_WORD = 6;

	private final long[] words;

	private static int wordIndex(int bitIndex) {
		return bitIndex >> ADDRESS_BITS_PER_WORD;
	}

	private ImmutableBitSet(long[] words) {
		this.words = words;
	}

	public static ImmutableBitSet valueOf(BitSet bitSet) {
		final long[] words = bitSet.toLongArray();
		return words.length == 0 ? EMPTY : new ImmutableBitSet( words );
	}

	public boolean get(int bitIndex) {
		int wordIndex = wordIndex( bitIndex );
		return wordIndex < words.length && ( ( words[wordIndex] & ( 1L << bitIndex ) ) != 0 );
	}

	public boolean isEmpty() {
		return this == EMPTY;
	}

	public boolean contains(ImmutableBitSet set) {
		if ( words.length < set.words.length ) {
			return false;
		}
		// We check if every word from the given set is also contained in this set
		for ( int i = 0; i < set.words.length; i++ ) {
			if ( words[i] != ( set.words[i] | words[i] ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode( words );
	}

	@Override
	public boolean equals(Object obj) {
		if ( !( obj instanceof ImmutableBitSet ) ) {
			return false;
		}
		if ( this == obj ) {
			return true;
		}

		final ImmutableBitSet set = (ImmutableBitSet) obj;
		return Arrays.equals( words, set.words );
	}

}

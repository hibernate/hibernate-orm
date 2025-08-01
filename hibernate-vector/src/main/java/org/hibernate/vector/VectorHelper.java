/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.BitSet;

/**
 * Helper for vector related functionality.
 *
 * @since 7.1
 */
public class VectorHelper {

	private static final float[] EMPTY_FLOAT_ARRAY = new float[0];

	public static @Nullable float[] parseFloatVector(@Nullable String string) {
		if ( string == null ) {
			return null;
		}
		if ( string.length() == 2 ) {
			return EMPTY_FLOAT_ARRAY;
		}
		final BitSet commaPositions = new BitSet();
		int size = 1;
		for ( int i = 1; i < string.length(); i++ ) {
			final char c = string.charAt( i );
			if ( c == ',' ) {
				commaPositions.set( i );
				size++;
			}
		}
		final float[] result = new float[size];
		int doubleStartIndex = 1;
		int commaIndex;
		int index = 0;
		while ( ( commaIndex = commaPositions.nextSetBit( doubleStartIndex ) ) != -1 ) {
			result[index++] = Float.parseFloat( string.substring( doubleStartIndex, commaIndex ) );
			doubleStartIndex = commaIndex + 1;
		}
		result[index] = Float.parseFloat( string.substring( doubleStartIndex, string.length() - 1 ) );
		return result;
	}

	public static @Nullable float[] parseFloatVector(@Nullable byte[] bytes) {
		if ( bytes == null ) {
			return null;
		}
		if ( bytes.length == 0 ) {
			return EMPTY_FLOAT_ARRAY;
		}
		if ( (bytes.length & 3) != 0 ) {
			throw new IllegalArgumentException(
					"Invalid byte array length. Expected a multiple of 4 but got: " + bytes.length );
		}
		final float[] result = new float[bytes.length >> 2];
		for ( int i = 0, resultLength = result.length; i < resultLength; i++ ) {
			final int offset = i << 2;
			final int asInt = (bytes[offset] & 0xFF)
							| ((bytes[offset + 1] & 0xFF) << 8)
							| ((bytes[offset + 2] & 0xFF) << 16)
							| ((bytes[offset + 3] & 0xFF) << 24);
			result[i] = Float.intBitsToFloat( asInt );
		}
		return result;
	}
}

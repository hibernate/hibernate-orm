/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

/**
 * Helper for vector related functionality.
 *
 * @since 7.2
 */
public class VectorHelper {

	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
	private static final float[] EMPTY_FLOAT_ARRAY = new float[0];
	private static final double[] EMPTY_DOUBLE_ARRAY = new double[0];

	public static @Nullable byte[] parseByteVector(@Nullable String string) {
		if ( string == null ) {
			return null;
		}
		if ( string.length() == 2 ) {
			return EMPTY_BYTE_ARRAY;
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
		final byte[] result = new byte[size];
		int doubleStartIndex = 1;
		int commaIndex;
		int index = 0;
		while ( ( commaIndex = commaPositions.nextSetBit( doubleStartIndex ) ) != -1 ) {
			result[index++] = Byte.parseByte( string.substring( doubleStartIndex, commaIndex ) );
			doubleStartIndex = commaIndex + 1;
		}
		result[index] = Byte.parseByte( string.substring( doubleStartIndex, string.length() - 1 ) );
		return result;
	}

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

	public static @Nullable double[] parseDoubleVector(@Nullable String string) {
		if ( string == null ) {
			return null;
		}
		if ( string.length() == 2 ) {
			return EMPTY_DOUBLE_ARRAY;
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
		final double[] result = new double[size];
		int doubleStartIndex = 1;
		int commaIndex;
		int index = 0;
		while ( ( commaIndex = commaPositions.nextSetBit( doubleStartIndex ) ) != -1 ) {
			result[index++] = Double.parseDouble( string.substring( doubleStartIndex, commaIndex ) );
			doubleStartIndex = commaIndex + 1;
		}
		result[index] = Double.parseDouble( string.substring( doubleStartIndex, string.length() - 1 ) );
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

	public static byte[] parseBitString(String bitString) {
		assert new BigInteger( "1" + bitString, 2 ).bitLength() == bitString.length() + 1;
		final int fullBytesCount = bitString.length() >> 3;
		final int fullBytesStartPosition = ((bitString.length() & 7) == 0 ? 0 : 1);
		final int byteCount = fullBytesCount + fullBytesStartPosition;
		final byte[] bytes = new byte[byteCount];
		final int fullBytesBitCount = fullBytesCount << 3;
		final int leadingBits = bitString.length() - fullBytesBitCount;
		if ( leadingBits > 0 ) {
			for  (int i = 0; i < leadingBits; i++ ) {
				bytes[0] |= (byte) (((bitString.charAt( i ) - 48)) << (7 - i));
			}
		}
		for ( int i = fullBytesStartPosition; i < fullBytesCount; i ++ ) {
			bytes[i] = (byte) (
					((bitString.charAt( i * 8 + 0 ) - 48) << 7)
					| ((bitString.charAt( i * 8 + 1 ) - 48) << 6)
					| ((bitString.charAt( i * 8 + 2 ) - 48) << 5)
					| ((bitString.charAt( i * 8 + 3 ) - 48) << 4)
					| ((bitString.charAt( i * 8 + 4 ) - 48) << 3)
					| ((bitString.charAt( i * 8 + 5 ) - 48) << 2)
					| ((bitString.charAt( i * 8 + 6 ) - 48) << 1)
					| ((bitString.charAt( i * 8 + 7 ) - 48) << 0)
			);
		}
		return bytes;
	}

	public static String toBitString(byte[] bytes) {
		final byte[] bitBytes = new byte[bytes.length * 8];
		for ( int i = 0; i < bytes.length; i++ ) {
			final byte b = bytes[i];
			bitBytes[i * 8 + 0] = (byte) (((b >>> 7) & 1) + 48);
			bitBytes[i * 8 + 1] = (byte) (((b >>> 6) & 1) + 48);
			bitBytes[i * 8 + 2] = (byte) (((b >>> 5) & 1) + 48);
			bitBytes[i * 8 + 3] = (byte) (((b >>> 4) & 1) + 48);
			bitBytes[i * 8 + 4] = (byte) (((b >>> 3) & 1) + 48);
			bitBytes[i * 8 + 5] = (byte) (((b >>> 2) & 1) + 48);
			bitBytes[i * 8 + 6] = (byte) (((b >>> 1) & 1) + 48);
			bitBytes[i * 8 + 7] = (byte) (((b >>> 0) & 1) + 48);
		}
		return new String( bitBytes, StandardCharsets.UTF_8 );
	}
}

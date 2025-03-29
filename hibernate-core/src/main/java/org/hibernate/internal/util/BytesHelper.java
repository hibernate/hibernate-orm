/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;


public final class BytesHelper {

	private BytesHelper() {
	}

	/**
	 * Custom algorithm used to generate an {@code int} from a series of bytes.
	 *
	 * @implNote This is different to interpreting the incoming bytes as an {@code int}!
	 *
	 * @param bytes The bytes to use in generating the int.
	 *
	 * @return The generated int.
	 */
	public static int toInt(byte[] bytes) {
		int result = 0;
		for ( int i = 0; i < 4; i++ ) {
			result = ( result << 8 ) - Byte.MIN_VALUE + (int) bytes[i];
		}
		return result;
	}

	/**
	 * Interpret a short as its binary form
	 *
	 * @param shortValue The short to interpret to binary
	 *
	 * @return The binary
	 */
	public static byte[] fromShort(int shortValue) {
		byte[] bytes = new byte[2];
		bytes[0] = (byte) ( shortValue >> 8 );
		bytes[1] = (byte) ( ( shortValue << 8 ) >> 8 );
		return bytes;
	}

	/**
	 * Interpret an int as its binary form
	 *
	 * @param intValue The int to interpret to binary
	 *
	 * @return The binary
	 */
	public static byte[] fromInt(int intValue) {
		byte[] bytes = new byte[4];
		bytes[0] = (byte) ( intValue >> 24 );
		bytes[1] = (byte) ( ( intValue << 8 ) >> 24 );
		bytes[2] = (byte) ( ( intValue << 16 ) >> 24 );
		bytes[3] = (byte) ( ( intValue << 24 ) >> 24 );
		return bytes;
	}

	/**
	 * Interpret a long as its binary form
	 *
	 * @param longValue The long to interpret to binary
	 *
	 * @return The binary
	 */
	public static byte[] fromLong(long longValue) {
		byte[] bytes = new byte[8];
		fromLong(longValue, bytes, 0);
		return bytes;
	}

	/**
	 * Interpret a long as its binary form
	 *
	 * @param longValue The long to interpret to binary
	 * @param dest the destination array.
	 * @param destPos starting position in the destination array.
	 */
	public static void fromLong(long longValue, byte[] dest, int destPos) {

		dest[destPos] = (byte) ( longValue >> 56 );
		dest[destPos + 1] = (byte) ( ( longValue << 8 ) >> 56 );
		dest[destPos + 2] = (byte) ( ( longValue << 16 ) >> 56 );
		dest[destPos + 3] = (byte) ( ( longValue << 24 ) >> 56 );
		dest[destPos + 4] = (byte) ( ( longValue << 32 ) >> 56 );
		dest[destPos + 5] = (byte) ( ( longValue << 40 ) >> 56 );
		dest[destPos + 6] = (byte) ( ( longValue << 48 ) >> 56 );
		dest[destPos + 7] = (byte) ( ( longValue << 56 ) >> 56 );
	}

	/**
	 * Interpret the binary representation of a long.
	 *
	 * @param bytes The bytes to interpret.
	 *
	 * @return The long
	 */
	public static long asLong(byte[] bytes) {
		return asLong(bytes, 0);
	}

	/**
	 * Interpret the binary representation of a long.
	 *
	 * @param bytes The bytes to interpret.
	 * @param srcPos starting position in the source array.
	 *
	 * @return The long
	 */
	public static long asLong(byte[] bytes, int srcPos) {
		if ( bytes == null ) {
			return 0;
		}
		final int size = srcPos + 8;
		if ( bytes.length < size ) {
			throw new IllegalArgumentException( "Expecting 8 byte values to construct a long" );
		}
		long value = 0;
		for (int i=srcPos; i<size; i++) {
			value = (value << 8) | (bytes[i] & 0xff);
		}
		return value;
	}

	public static String toBinaryString(byte value) {
		String formatted = Integer.toBinaryString( value );
		if ( formatted.length() > 8 ) {
			formatted = formatted.substring( formatted.length() - 8 );
		}
		StringBuilder buf = new StringBuilder( "00000000" );
		buf.replace( 8 - formatted.length(), 8, formatted );
		return buf.toString();
	}

	public static String toBinaryString(int value) {
		String formatted = Long.toBinaryString( value );
		StringBuilder buf = new StringBuilder( StringHelper.repeat( '0', 32 ) );
		buf.replace( 64 - formatted.length(), 64, formatted );
		return buf.toString();
	}

	public static String toBinaryString(long value) {
		String formatted = Long.toBinaryString( value );
		StringBuilder buf = new StringBuilder( StringHelper.repeat( '0', 64 ) );
		buf.replace( 64 - formatted.length(), 64, formatted );
		return buf.toString();
	}
}

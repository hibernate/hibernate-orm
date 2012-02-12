/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.internal.util;


public final class BytesHelper {

	private BytesHelper() {
	}

	/**
	 * Custom algorithm used to generate an int from a series of bytes.
	 * <p/>
	 * NOTE : this is different than interpreting the incoming bytes as an int value!
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
		bytes[0] = (byte) ( longValue >> 56 );
		bytes[1] = (byte) ( ( longValue << 8 ) >> 56 );
		bytes[2] = (byte) ( ( longValue << 16 ) >> 56 );
		bytes[3] = (byte) ( ( longValue << 24 ) >> 56 );
		bytes[4] = (byte) ( ( longValue << 32 ) >> 56 );
		bytes[5] = (byte) ( ( longValue << 40 ) >> 56 );
		bytes[6] = (byte) ( ( longValue << 48 ) >> 56 );
		bytes[7] = (byte) ( ( longValue << 56 ) >> 56 );
		return bytes;
	}

	/**
	 * Interpret the binary representation of a long.
	 *
	 * @param bytes The bytes to interpret.
	 *
	 * @return The long
	 */
	public static long asLong(byte[] bytes) {
		if ( bytes == null ) {
			return 0;
		}
		if ( bytes.length != 8 ) {
			throw new IllegalArgumentException( "Expecting 8 byte values to construct a long" );
		}
		long value = 0;
        for (int i=0; i<8; i++) {
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

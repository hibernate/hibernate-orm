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
package org.hibernate.id.uuid;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.hibernate.internal.util.BytesHelper;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public final class Helper {
	private Helper() {
	}

	// IP ADDRESS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private static final byte[] ADDRESS_BYTES;
	private static final int ADDRESS_INT;
	private static final String ADDRESS_HEX_STRING;

	static {
		byte[] address;
		try {
			address = InetAddress.getLocalHost().getAddress();
		}
		catch ( Exception e ) {
			address = new byte[4];
		}
		ADDRESS_BYTES = address;
		ADDRESS_INT = BytesHelper.toInt( ADDRESS_BYTES );
		ADDRESS_HEX_STRING = format( ADDRESS_INT );
	}

	public static byte[] getAddressBytes() {
		return ADDRESS_BYTES;
	}

	public static int getAddressInt() {
		return ADDRESS_INT;
	}

	public static String getAddressHexString() {
		return ADDRESS_HEX_STRING;
	}


	// JVM identifier ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private static final byte[] JVM_IDENTIFIER_BYTES;
	private static final int JVM_IDENTIFIER_INT;
	private static final String JVM_IDENTIFIER_HEX_STRING;

	static {
		JVM_IDENTIFIER_INT = (int) ( System.currentTimeMillis() >>> 8 );
		JVM_IDENTIFIER_BYTES = BytesHelper.fromInt( JVM_IDENTIFIER_INT );
		JVM_IDENTIFIER_HEX_STRING = format( JVM_IDENTIFIER_INT );
	}

	public static byte[] getJvmIdentifierBytes() {
		return JVM_IDENTIFIER_BYTES;
	}

	public static int getJvmIdentifierInt() {
		return JVM_IDENTIFIER_INT;
	}

	public static String getJvmIdentifierHexString() {
		return JVM_IDENTIFIER_HEX_STRING;
	}


	// counter ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private static short counter = (short) 0;

	/**
	 * Unique in a millisecond for this JVM instance (unless there are > Short.MAX_VALUE instances created in a
	 * millisecond)
	 */
	public static short getCountShort() {
		synchronized ( Helper.class ) {
			if ( counter < 0 ) {
				counter = 0;
			}
			return counter++;
		}
	}

	public static byte[] getCountBytes() {
		return BytesHelper.fromShort( getCountShort() );
	}


	// Helper methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static String format(int value) {
		final String formatted = Integer.toHexString( value );
		StringBuilder buf = new StringBuilder( "00000000" );
		buf.replace( 8 - formatted.length(), 8, formatted );
		return buf.toString();
	}

	public static String format(short value) {
		String formatted = Integer.toHexString( value );
		StringBuilder buf = new StringBuilder( "0000" );
		buf.replace( 4 - formatted.length(), 4, formatted );
		return buf.toString();
	}


	public static void main(String[] args) throws UnknownHostException {
		byte[] addressBytes = InetAddress.getLocalHost().getAddress();
		System.out.println( "Raw ip address bytes : " + addressBytes.toString() );

		int addressInt = BytesHelper.toInt( addressBytes );
		System.out.println( "ip address int : " + addressInt );

		String formatted = Integer.toHexString( addressInt );
		StringBuilder buf = new StringBuilder( "00000000" );
		buf.replace( 8 - formatted.length(), 8, formatted );
		String addressHex = buf.toString();
		System.out.println( "ip address hex : " + addressHex );
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.uuid;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.hibernate.internal.build.AllowSysOut;
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

}

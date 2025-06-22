/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import java.net.InetAddress;

import org.hibernate.internal.util.BytesHelper;

/**
 * The base class for identifier generators that use a UUID algorithm. This
 * class implements the algorithm, subclasses define the identifier
 * format.
 *
 * @see UUIDHexGenerator
 * @author Gavin King
 *
 * @deprecated since {@link UUIDHexGenerator} is deprecated
 */
@Deprecated(since = "6")
public abstract class AbstractUUIDGenerator implements IdentifierGenerator {

	private static final int IP;
	static {
		int ipadd;
		try {
			ipadd = BytesHelper.toInt( InetAddress.getLocalHost().getAddress() );
		}
		catch (Exception e) {
			ipadd = 0;
		}
		IP = ipadd;
	}

	private static short counter = (short) 0;
	private static final int JVM = (int) ( System.currentTimeMillis() >>> 8 );

	public AbstractUUIDGenerator() {
	}

	/**
	 * Unique across JVMs on this machine (unless they load this class
	 * in the same quarter-second, which is very unlikely)
	 */
	protected int getJVM() {
		return JVM;
	}

	/**
	 * Unique in a millisecond for this JVM instance (unless there
	 * are more than {@value Short#MAX_VALUE} instances created in
	 * a millisecond)
	 */
	protected short getCount() {
		synchronized(AbstractUUIDGenerator.class) {
			if ( counter < 0 ) {
				counter=0;
			}
			return counter++;
		}
	}

	/**
	 * Unique in a local network
	 */
	protected int getIP() {
		return IP;
	}

	/**
	 * Unique down to millisecond
	 */
	protected short getHiTime() {
		return (short) ( System.currentTimeMillis() >>> 32 );
	}

	protected int getLoTime() {
		return (int) System.currentTimeMillis();
	}
}

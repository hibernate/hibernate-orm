/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.usertype.inet;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * @author Vlad Mihalcea
 */
public class Inet {

	private final String address;

	public Inet(String address) {
		this.address = address;
	}

	public String getAddress() {
		return address;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		return Objects.equals( address, Inet.class.cast( o ).address );
	}

	@Override
	public int hashCode() {
		return Objects.hash( address );
	}

	public InetAddress toInetAddress() {
		try {
			String host = address.replaceAll( "\\/.*$", "" );
			return Inet4Address.getByName( host );
		}
		catch (UnknownHostException e) {
			throw new IllegalStateException( e );
		}
	}

	@Override
	public String toString() {
		return address;
	}
}

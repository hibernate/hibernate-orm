/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.usertype.internal;

import java.io.Serializable;

import org.hibernate.usertype.CompositeUserType;

/**
 * @author Christian Beikov
 */
public abstract class AbstractTimeZoneStorageCompositeUserType<T> implements CompositeUserType<T> {

	public static final String INSTANT_NAME = "instant";
	public static final String ZONE_OFFSET_NAME = "zoneOffset";

	@Override
	public boolean equals(T x, T y) {
		return x.equals( y );
	}

	@Override
	public int hashCode(T x) {
		return x.hashCode();
	}

	@Override
	public T deepCopy(T value) {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(T value) {
		return (Serializable) value;
	}

	@Override
	public T assemble(Serializable cached, Object owner) {
		//noinspection unchecked
		return (T) cached;
	}

	@Override
	public T replace(T detached, T managed, Object owner) {
		return detached;
	}

}

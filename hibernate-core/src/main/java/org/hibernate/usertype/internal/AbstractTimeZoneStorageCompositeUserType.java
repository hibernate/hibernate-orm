/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
	public boolean equals(Object x, Object y) {
		return x.equals( y );
	}

	@Override
	public int hashCode(Object x) {
		return x.hashCode();
	}

	@Override
	public Object deepCopy(Object value) {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Object value) {
		return (Serializable) value;
	}

	@Override
	public Object assemble(Serializable cached, Object owner) {
		return cached;
	}

	@Override
	public Object replace(Object detached, Object managed, Object owner) {
		return detached;
	}

}

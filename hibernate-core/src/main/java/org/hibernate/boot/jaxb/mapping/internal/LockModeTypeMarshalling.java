/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.internal;

import jakarta.persistence.LockModeType;

/**
 * JAXB marshalling for {@link LockModeType}
 *
 * @author Steve Ebersole
 */
public class LockModeTypeMarshalling {
	public static LockModeType fromXml(String name) {
		return name == null ? null : LockModeType.valueOf( name );
	}

	public static String toXml(LockModeType lockModeType) {
		return lockModeType == null ? null : lockModeType.name();
	}
}

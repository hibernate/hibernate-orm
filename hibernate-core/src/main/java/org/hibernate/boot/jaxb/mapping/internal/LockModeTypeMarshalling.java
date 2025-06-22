/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

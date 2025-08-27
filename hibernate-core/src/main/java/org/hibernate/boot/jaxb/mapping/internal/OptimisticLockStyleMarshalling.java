/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;


import org.hibernate.engine.OptimisticLockStyle;

/**
 * JAXB marshalling for {@link OptimisticLockStyle}
 *
 * @author Steve Ebersole
 */
public class OptimisticLockStyleMarshalling {
	public static OptimisticLockStyle fromXml(String name) {
		return name == null ? null : OptimisticLockStyle.valueOf( name );
	}

	public static String toXml(OptimisticLockStyle lockMode) {
		return lockMode == null ? null : lockMode.name();
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import org.hibernate.cache.spi.access.AccessType;

/**
 * JAXB marshalling for Hibernate's {@link AccessType}
 *
 * @author Steve Ebersole
 */
public class CacheAccessTypeMarshalling {
	public static AccessType fromXml(String name) {
		return name == null ? null : AccessType.valueOf( name );
	}

	public static String toXml(AccessType accessType) {
		return accessType == null ? null : accessType.name();
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.internal;

import org.hibernate.cache.spi.access.AccessType;

/**
 * @author Steve Ebersole
 */
public class CacheAccessTypeConverter {
	public static AccessType fromXml(String name) {
		return AccessType.fromExternalName( name );
	}

	public static String toXml(AccessType accessType) {
		return accessType.getExternalName();
	}
}

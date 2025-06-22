/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;


import org.hibernate.CacheMode;

/**
 * JAXB marshalling for Hibernate's {@link CacheMode}
 *
 * @author Steve Ebersole
 */
public class CacheModeMarshalling {
	public static CacheMode fromXml(String name) {
		return name == null ? CacheMode.NORMAL : CacheMode.valueOf( name );
	}

	public static String toXml(CacheMode cacheMode) {
		return cacheMode == null ? null : cacheMode.name();
	}
}

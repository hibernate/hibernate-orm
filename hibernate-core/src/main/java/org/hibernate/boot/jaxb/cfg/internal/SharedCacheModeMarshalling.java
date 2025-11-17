/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.cfg.internal;

import org.hibernate.internal.util.StringHelper;

import jakarta.persistence.SharedCacheMode;

/**
 * @author Steve Ebersole
 */
public class SharedCacheModeMarshalling {
	public static SharedCacheMode fromXml(String name) {
		if ( StringHelper.isEmpty( name ) ) {
			return SharedCacheMode.UNSPECIFIED;
		}
		return SharedCacheMode.valueOf( name );
	}

	public static String toXml(SharedCacheMode sharedCacheMode) {
		if ( sharedCacheMode == null ) {
			return null;
		}
		return sharedCacheMode.name();
	}
}

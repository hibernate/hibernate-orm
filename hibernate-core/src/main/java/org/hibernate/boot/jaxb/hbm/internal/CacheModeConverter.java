/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.internal;

import java.util.Locale;

import org.hibernate.CacheMode;

/**
 * @author Steve Ebersole
 */
public class CacheModeConverter {
	public static CacheMode fromXml(String name) {
		for ( CacheMode mode : CacheMode.values() ) {
			if ( mode.name().equalsIgnoreCase( name ) ) {
				return mode;
			}
		}
		return CacheMode.NORMAL;
	}

	public static String toXml(CacheMode cacheMode) {
		return cacheMode.name().toLowerCase( Locale.ENGLISH );
	}
}

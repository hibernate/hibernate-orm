/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.cfg.internal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.type.BasicType;

import java.util.Comparator;

class ComparatorUtil {
	@NonNull
	static <T> Comparator<Object> versionComparator(BasicType<T> type) {
		return (u, v) -> {
			final var descriptor = type.getJavaTypeDescriptor();
			final T x;
			final T y;
			try {
				x = descriptor.cast( u );
				y = descriptor.cast( v );
			}
			catch (Exception e) {
				throw new IllegalArgumentException( "Cached version was not of type " + type.getName(), e );
			}
			return descriptor.getComparator().compare( x, y );
		};
	}
}

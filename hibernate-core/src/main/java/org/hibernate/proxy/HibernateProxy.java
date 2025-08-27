/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.proxy;

import java.io.Serial;
import java.io.Serializable;

import org.hibernate.Internal;
import org.hibernate.engine.spi.PrimeAmongSecondarySupertypes;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Interface implemented directly by entity proxies, exposing
 * access to the associated {@link LazyInitializer}.
 *
 * @author Gavin King
 */
public interface HibernateProxy extends Serializable, PrimeAmongSecondarySupertypes {

	/**
	 * Extract the {@link LazyInitializer} from the given object,
	 * if and only if the object is actually a proxy. Otherwise,
	 * return a null value.
	 *
	 * @param object any reference to an entity
	 * @return the associated {@link LazyInitializer} if the given
	 *         object is a proxy, or {@code null} otherwise.
	 */
	static @Nullable LazyInitializer extractLazyInitializer(final @Nullable Object object) {
		if ( object instanceof PrimeAmongSecondarySupertypes t ) {
			final HibernateProxy hibernateProxy = t.asHibernateProxy();
			if ( hibernateProxy != null ) {
				return hibernateProxy.getHibernateLazyInitializer();
			}
		}
		return null;
	}

	/**
	 * Perform serialization-time write-replacement of this proxy.
	 *
	 * @return The serializable proxy replacement.
	 */
	@Serial
	Object writeReplace();

	/**
	 * Get the {@linkplain LazyInitializer lazy initialization handler}
	 * for this object.
	 *
	 * @return The associated {@link LazyInitializer}.
	 */
	LazyInitializer getHibernateLazyInitializer();

	/**
	 * Special internal contract to optimize type checking.
	 *
	 * @see PrimeAmongSecondarySupertypes
	 *
	 * @return this instance
	 */
	@Internal
	@Override
	default HibernateProxy asHibernateProxy() {
		return this;
	}
}

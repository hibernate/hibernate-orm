/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.property.access.spi.PropertyAccessorService;

/**
 * Default {@link PropertyAccessorService} implementation backed by ByteBuddy.
 *
 * <p>Wraps the ByteBuddy factory in {@link HibernateOrmAccessorFactory} which
 * delegates individual accessors to ByteBuddy and provides ORM-specific
 * multi-value accessor implementations with enhancement logic.
 */
public class ByteBuddyPropertyAccessorService implements PropertyAccessorService {

	private final HibernateOrmAccessorFactory factory;

	public ByteBuddyPropertyAccessorService(Map<String, Object> configurationValues) {
		final MethodHandles.Lookup lookup = MethodHandles.lookup();
		this.factory = new HibernateOrmAccessorFactory(
				lookup,
				configurationValues
		);
	}

	@Override
	public AccessorFactory hibernateAccessorFactory() {
		return factory;
	}
}

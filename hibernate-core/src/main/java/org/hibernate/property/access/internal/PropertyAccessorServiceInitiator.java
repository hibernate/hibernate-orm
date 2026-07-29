/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.models.accessor.HibernateAccessorFactory;
import org.hibernate.property.access.spi.PropertyAccessorService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import java.util.Map;

/**
 * {@link StandardServiceInitiator} for {@link PropertyAccessorService}.
 *
 * <p>Defaults to the ByteBuddy-based implementation. Quarkus and similar
 * frameworks replace via {@link java.util.ServiceLoader} discovery.
 */
public class PropertyAccessorServiceInitiator implements StandardServiceInitiator<PropertyAccessorService> {

	public static final PropertyAccessorServiceInitiator INSTANCE = new PropertyAccessorServiceInitiator();

	@Nonnull
	@Override
	public Class<PropertyAccessorService> getServiceInitiated() {
		return PropertyAccessorService.class;
	}

	@Override
	public PropertyAccessorService initiateService(
			@Nonnull Map<String, Object> configurationValues,
			@Nonnull ServiceRegistryImplementor registry) {
		return new PropertyAccessorService() {
			@Override
			public HibernateAccessorFactory hibernateAccessorFactory() {
				return HibernateAccessorFactory.reflection();
			}
		};
	}
}

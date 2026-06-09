/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import java.util.Map;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class PropertyAccessStrategyResolverInitiator implements StandardServiceInitiator<PropertyAccessStrategyResolver> {
	/**
	 * Singleton access
	 */
	public static final PropertyAccessStrategyResolverInitiator INSTANCE = new PropertyAccessStrategyResolverInitiator();

	@Nonnull
	@Override
	public Class<PropertyAccessStrategyResolver> getServiceInitiated() {
		return PropertyAccessStrategyResolver.class;
	}

	@Override
	public PropertyAccessStrategyResolver initiateService(@Nonnull Map<String, Object> configurationValues, @Nonnull ServiceRegistryImplementor registry) {
		return new PropertyAccessStrategyResolverStandardImpl( registry );
	}
}

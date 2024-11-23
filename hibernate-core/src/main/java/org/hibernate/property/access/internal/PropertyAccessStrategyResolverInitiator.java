/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import java.util.Map;

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

	@Override
	public Class<PropertyAccessStrategyResolver> getServiceInitiated() {
		return PropertyAccessStrategyResolver.class;
	}

	@Override
	public PropertyAccessStrategyResolver initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		return new PropertyAccessStrategyResolverStandardImpl( registry );
	}
}

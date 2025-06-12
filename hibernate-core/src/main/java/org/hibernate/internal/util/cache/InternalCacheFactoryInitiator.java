/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.cache;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import java.util.Map;

public class InternalCacheFactoryInitiator implements StandardServiceInitiator<InternalCacheFactory> {

	/**
	 * Singleton access
	 */
	public static final InternalCacheFactoryInitiator INSTANCE = new InternalCacheFactoryInitiator();

	private InternalCacheFactoryInitiator() {}

	@Override
	public InternalCacheFactory initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		return new InternalCacheFactoryImpl();
	}

	@Override
	public Class<InternalCacheFactory> getServiceInitiated() {
		return InternalCacheFactory.class;
	}
}

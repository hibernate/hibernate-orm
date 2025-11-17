/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Most commonly the {@link ProxyFactoryFactory} will depend directly on the chosen {@link BytecodeProvider},
 * however by registering them as two separate services we can allow to override either one
 * or both of them.
 * @author Sanne Grinovero
 */
public final class ProxyFactoryFactoryInitiator implements StandardServiceInitiator<ProxyFactoryFactory> {

	/**
	 * Singleton access
	 */
	public static final StandardServiceInitiator<ProxyFactoryFactory> INSTANCE = new ProxyFactoryFactoryInitiator();

	@Override
	public ProxyFactoryFactory initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		return registry.requireService( BytecodeProvider.class ).getProxyFactoryFactory();
	}

	@Override
	public Class<ProxyFactoryFactory> getServiceInitiated() {
		return ProxyFactoryFactory.class;
	}
}

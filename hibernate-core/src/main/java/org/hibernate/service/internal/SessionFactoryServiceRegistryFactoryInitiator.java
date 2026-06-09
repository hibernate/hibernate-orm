/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.internal;

import java.util.Map;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistryFactory;

/**
 * @author Steve Ebersole
 */
public class SessionFactoryServiceRegistryFactoryInitiator implements StandardServiceInitiator<SessionFactoryServiceRegistryFactory> {
	public static final SessionFactoryServiceRegistryFactoryInitiator INSTANCE = new SessionFactoryServiceRegistryFactoryInitiator();

	@Nonnull
	@Override
	public Class<SessionFactoryServiceRegistryFactory> getServiceInitiated() {
		return SessionFactoryServiceRegistryFactory.class;
	}

	@Override
	public SessionFactoryServiceRegistryFactoryImpl initiateService(@Nonnull Map<String, Object> configurationValues, @Nonnull ServiceRegistryImplementor registry) {
		return new SessionFactoryServiceRegistryFactoryImpl( registry );
	}
}

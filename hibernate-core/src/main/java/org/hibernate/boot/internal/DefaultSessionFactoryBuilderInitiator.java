/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import java.util.Map;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.spi.SessionFactoryBuilderService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class DefaultSessionFactoryBuilderInitiator implements StandardServiceInitiator<SessionFactoryBuilderService> {

	public static final DefaultSessionFactoryBuilderInitiator INSTANCE = new DefaultSessionFactoryBuilderInitiator();

	private DefaultSessionFactoryBuilderInitiator() {
	}

	@Override
	public SessionFactoryBuilderService initiateService(@Nonnull Map<String, Object> configurationValues, @Nonnull ServiceRegistryImplementor registry) {
		return DefaultSessionFactoryBuilderService.INSTANCE;
	}

	@Nonnull
	@Override
	public Class<SessionFactoryBuilderService> getServiceInitiated() {
		return SessionFactoryBuilderService.class;
	}

}

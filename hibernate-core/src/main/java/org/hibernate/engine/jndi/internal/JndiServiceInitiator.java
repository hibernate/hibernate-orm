/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jndi.internal;

import java.util.Map;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Standard initiator for the standard {@link JndiService} service
 *
 * @author Steve Ebersole
 */
public final class JndiServiceInitiator implements StandardServiceInitiator<JndiService> {
	/**
	 * Singleton access
	 */
	public static final JndiServiceInitiator INSTANCE = new JndiServiceInitiator();

	@Nonnull
	@Override
	public Class<JndiService> getServiceInitiated() {
		return JndiService.class;
	}

	@Override
	public JndiService initiateService(@Nonnull Map<String, Object> configurationValues, @Nonnull ServiceRegistryImplementor registry) {
		return new JndiServiceImpl( configurationValues );
	}
}

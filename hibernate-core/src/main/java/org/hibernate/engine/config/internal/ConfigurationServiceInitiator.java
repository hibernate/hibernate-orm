/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.config.internal;

import java.util.Map;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * The {@link org.hibernate.service.spi.ServiceInitiator} for the {@link ConfigurationService}.
 *
 * @author Steve Ebersole
 */
public class ConfigurationServiceInitiator implements StandardServiceInitiator<ConfigurationService> {
	/**
	 * Singleton access
	 */
	public static final ConfigurationServiceInitiator INSTANCE = new ConfigurationServiceInitiator();

	@Override
	public ConfigurationService initiateService(@Nonnull Map<String, Object> configurationValues, @Nonnull ServiceRegistryImplementor registry) {
		return new ConfigurationServiceImpl( configurationValues );
	}

	@Nonnull
	@Override
	public Class<ConfigurationService> getServiceInitiated() {
		return ConfigurationService.class;
	}
}

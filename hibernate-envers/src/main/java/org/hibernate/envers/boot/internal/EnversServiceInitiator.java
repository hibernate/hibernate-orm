/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.internal;

import java.util.Map;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class EnversServiceInitiator implements StandardServiceInitiator<EnversService> {
	/**
	 * Singleton access
	 */
	public static final EnversServiceInitiator INSTANCE = new EnversServiceInitiator();

	@Override
	public EnversService initiateService(
			@Nonnull Map<String, Object> configurationValues,
			@Nonnull ServiceRegistryImplementor registry) {
		return new EnversServiceImpl();
	}

	@Nonnull
	@Override
	public Class<EnversService> getServiceInitiated() {
		return EnversService.class;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.internal;

import java.util.Map;

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
			Map<String, Object> configurationValues,
			ServiceRegistryImplementor registry) {
		return new EnversServiceImpl();
	}

	@Override
	public Class<EnversService> getServiceInitiated() {
		return EnversService.class;
	}
}

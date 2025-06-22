/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducerProvider;

/**
 * Initiator for {@link JdbcValuesMappingProducerProviderStandard}
 *
 * @author Steve Ebersole
 */
public class JdbcValuesMappingProducerProviderInitiator
		implements StandardServiceInitiator<JdbcValuesMappingProducerProvider> {
	/**
	 * Singleton access
	 */
	public static final JdbcValuesMappingProducerProviderInitiator INSTANCE = new JdbcValuesMappingProducerProviderInitiator();

	@Override
	public JdbcValuesMappingProducerProvider initiateService(
			Map<String, Object> configurationValues,
			ServiceRegistryImplementor registry) {
		return JdbcValuesMappingProducerProviderStandard.INSTANCE;
	}

	@Override
	public Class<JdbcValuesMappingProducerProvider> getServiceInitiated() {
		return JdbcValuesMappingProducerProvider.class;
	}
}

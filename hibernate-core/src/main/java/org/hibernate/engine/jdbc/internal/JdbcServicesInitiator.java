/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Standard initiator for the standard {@link JdbcServices} service
 *
 * TODO : should this maybe be a SessionFactory service?
 *
 * @author Steve Ebersole
 */
public class JdbcServicesInitiator implements StandardServiceInitiator<JdbcServices> {
	/**
	 * Singleton access
	 */
	public static final JdbcServicesInitiator INSTANCE = new JdbcServicesInitiator();

	@Override
	public Class<JdbcServices> getServiceInitiated() {
		return JdbcServices.class;
	}

	@Override
	public JdbcServices initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		return new JdbcServicesImpl();
	}
}

/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.dialect.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Standard initiator for the standard {@link DialectFactory} service
 *
 * @author Steve Ebersole
 */
public class DialectFactoryInitiator implements StandardServiceInitiator<DialectFactory> {
	/**
	 * Singleton access
	 */
	public static final DialectFactoryInitiator INSTANCE = new DialectFactoryInitiator();

	@Override
	public Class<DialectFactory> getServiceInitiated() {
		return DialectFactory.class;
	}

	@Override
	public DialectFactory initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		return new DialectFactoryImpl();
	}
}

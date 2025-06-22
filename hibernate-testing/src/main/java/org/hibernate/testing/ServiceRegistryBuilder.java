/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing;

import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.util.ServiceRegistryUtil;

/**
 * @author Steve Ebersole
 */
public final class ServiceRegistryBuilder {
	private ServiceRegistryBuilder() {
	}

	public static StandardServiceRegistryImpl buildServiceRegistry() {
		return buildServiceRegistry( PropertiesHelper.map( Environment.getProperties() ) );
	}

	public static StandardServiceRegistryImpl buildServiceRegistry(Map<String,Object> serviceRegistryConfig) {
		return (StandardServiceRegistryImpl) ServiceRegistryUtil.serviceRegistryBuilder()
				.applySettings( serviceRegistryConfig )
				.build();
	}

	public static StandardServiceRegistryImpl buildServiceRegistry(Properties serviceRegistryConfig) {
		return (StandardServiceRegistryImpl) ServiceRegistryUtil.serviceRegistryBuilder()
				.applySettings( serviceRegistryConfig )
				.build();
	}

	public static void destroy(ServiceRegistry serviceRegistry) {
		( (StandardServiceRegistryImpl) serviceRegistry ).destroy();
	}
}

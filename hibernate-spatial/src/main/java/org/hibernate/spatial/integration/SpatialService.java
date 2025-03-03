/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.integration;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.Service;
import org.hibernate.spatial.HSMessageLogger;

import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;

/**
 * Central service for spatial integration
 *
 * @author Karel Maesen, Geovise BVBA
 * @author Steve Ebersole
 */
public class SpatialService implements Service {
	/**
	 * The name of the configuration setting used to control whether the spatial integration
	 * is enabled.  Default is true
	 */
	public static final String INTEGRATION_ENABLED = "hibernate.integration.spatial.enabled";

	private static final HSMessageLogger log = Logger.getMessageLogger(
			MethodHandles.lookup(),
			HSMessageLogger.class,
			SpatialService.class.getName()
	);

	private boolean integrationEnabled;

	public SpatialService(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		this.integrationEnabled = ConfigurationHelper.getBoolean(
				INTEGRATION_ENABLED,
				serviceRegistryBuilder.getSettings(),
				true
		);
		log.spatialEnabled( integrationEnabled );
	}

	public boolean isEnabled() {
		return integrationEnabled;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.spatial.integration;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.Service;
import org.hibernate.spatial.HSMessageLogger;

import org.jboss.logging.Logger;

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

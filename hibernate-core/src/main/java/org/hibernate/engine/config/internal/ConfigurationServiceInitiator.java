/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.config.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * The ServiceInitiator for the ConfigurationService
 *
 * @author Steve Ebersole
 */
public class ConfigurationServiceInitiator implements StandardServiceInitiator<ConfigurationService> {
	/**
	 * Singleton access
	 */
	public static final ConfigurationServiceInitiator INSTANCE = new ConfigurationServiceInitiator();

	@Override
	public ConfigurationService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new ConfigurationServiceImpl( configurationValues );
	}

	@Override
	public Class<ConfigurationService> getServiceInitiated() {
		return ConfigurationService.class;
	}
}

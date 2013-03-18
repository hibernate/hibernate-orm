/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.spi;

import java.util.Properties;

import org.jboss.logging.Logger;

import org.hibernate.cache.CacheException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Settings;
import org.hibernate.cfg.SettingsFactory;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public abstract class AbstractRegionFactory implements RegionFactory, ServiceRegistryAwareService {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			AbstractRegionFactory.class.getName()
	);

	private ServiceRegistryImplementor serviceRegistry;
	private boolean isMinimalPutsEnabled;

	public ServiceRegistryImplementor getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.isMinimalPutsEnabled = serviceRegistry.getService( ConfigurationService.class ).getSetting( AvailableSettings.USE_MINIMAL_PUTS,
				StandardConverters.BOOLEAN, isMinimalPutsEnabledByDefault()
		);
		LOG.debugf( "Optimize cache for minimal puts: %s", SettingsFactory.enabledDisabled( isMinimalPutsEnabled ) );
	}

	@Override
	public void start(Settings settings, Properties properties) throws CacheException {
		start();
	}

	@Override
	public boolean isMinimalPutsEnabled() {
		return isMinimalPutsEnabled;
	}
}

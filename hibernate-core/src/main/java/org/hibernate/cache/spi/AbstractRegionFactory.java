package org.hibernate.cache.spi;

import java.util.Properties;

import org.jboss.logging.Logger;

import org.hibernate.cache.CacheException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Settings;
import org.hibernate.cfg.SettingsFactory;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.config.spi.ConfigurationService;
import org.hibernate.service.config.spi.StandardConverters;
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

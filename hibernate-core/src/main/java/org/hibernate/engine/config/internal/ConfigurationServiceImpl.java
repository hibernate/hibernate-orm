/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.config.internal;

import java.util.Collections;
import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.logging.Logger;

/**
 * The standard ConfigurationService implementation
 *
 * @author Steve Ebersole
 */
public class ConfigurationServiceImpl implements ConfigurationService, ServiceRegistryAwareService {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			ConfigurationServiceImpl.class.getName()
	);

	private final Map settings;
	private ServiceRegistryImplementor serviceRegistry;

	/**
	 * Constructs a ConfigurationServiceImpl
	 *
	 * @param settings The map of settings
	 */
	@SuppressWarnings( "unchecked" )
	public ConfigurationServiceImpl(Map settings) {
		this.settings = Collections.unmodifiableMap( settings );
	}

	@Override
	public Map getSettings() {
		return settings;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public <T> T getSetting(String name, Converter<T> converter) {
		return getSetting( name, converter, null );
	}

	@Override
	public <T> T getSetting(String name, Converter<T> converter, T defaultValue) {
		final Object value = settings.get( name );
		if ( value == null ) {
			return defaultValue;
		}

		return converter.convert( value );
	}

	@Override
	public <T> T getSetting(String name, Class<T> expected, T defaultValue) {
		final Object value = settings.get( name );
		final T target = cast( expected, value );
		return target !=null ? target : defaultValue;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T cast(Class<T> expected, Object candidate){
		if (candidate == null) {
			return null;
		}

		if ( expected.isInstance( candidate ) ) {
			return (T) candidate;
		}

		Class<T> target;
		if ( Class.class.isInstance( candidate ) ) {
			target = Class.class.cast( candidate );
		}
		else {
			try {
				target = serviceRegistry.getService( ClassLoaderService.class ).classForName( candidate.toString() );
			}
			catch ( ClassLoadingException e ) {
				LOG.debugf( "Unable to locate %s implementation class %s", expected.getName(), candidate.toString() );
				target = null;
			}
		}
		if ( target != null ) {
			try {
				return target.newInstance();
			}
			catch ( Exception e ) {
				LOG.debugf(
						"Unable to instantiate %s class %s", expected.getName(),
						target.getName()
				);
			}
		}
		return null;
	}


}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.config.internal;

import java.util.Collections;
import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;


import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

/**
 * The standard {@link ConfigurationService} implementation.
 *
 * @author Steve Ebersole
 */
public class ConfigurationServiceImpl implements ConfigurationService, ServiceRegistryAwareService {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( ConfigurationServiceImpl.class );

	private final Map<String, Object> settings;
	private ServiceRegistryImplementor serviceRegistry;

	/**
	 * Constructs a ConfigurationServiceImpl
	 *
	 * @param settings The map of settings
	 */
	public ConfigurationServiceImpl(Map<String, Object> settings) {
		this.settings = Collections.unmodifiableMap( settings );
	}

	@Override
	public Map<String, Object> getSettings() {
		return settings;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public <T> @Nullable T getSetting(String name, Converter<T> converter) {
		return getSetting( name, converter, null );
	}

	@Override
	public <T> @PolyNull T getSetting(String name, Converter<T> converter, @PolyNull T defaultValue) {
		final Object value = settings.get( name );
		return value == null ? defaultValue : converter.convert( value );
	}

	@Override
	public <T> @PolyNull T getSetting(String name, Class<T> expected, @PolyNull T defaultValue) {
		final Object value = settings.get( name );
		final T target = cast( expected, value );
		return target !=null ? target : defaultValue;
	}

	@SuppressWarnings("unchecked")
	public <T> @Nullable T cast(Class<T> expected, @Nullable Object candidate){
		if (candidate == null) {
			return null;
		}

		if ( expected.isInstance( candidate ) ) {
			return (T) candidate;
		}

		Class<T> target;
		if (candidate instanceof Class) {
			target = (Class<T>) candidate;
		}
		else {
			try {
				target = serviceRegistry.requireService( ClassLoaderService.class )
						.classForName( candidate.toString() );
			}
			catch ( ClassLoadingException e ) {
				log.debugf( "Unable to locate %s implementation class %s", expected.getName(), candidate.toString() );
				target = null;
			}
		}
		if ( target != null ) {
			try {
				return target.newInstance();
			}
			catch ( Exception e ) {
				log.debugf(
						"Unable to instantiate %s class %s", expected.getName(),
						target.getName()
				);
			}
		}
		return null;
	}


}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * Initiator for the {@link RegionFactory} service.
 *
 * @author Hardy Ferentschik
 * @author Brett Meyer
 */
public class RegionFactoryInitiator implements StandardServiceInitiator<RegionFactory> {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( RegionFactoryInitiator.class );

	/**
	 * Singleton access
	 */
	public static final RegionFactoryInitiator INSTANCE = new RegionFactoryInitiator();

	@Override
	public Class<RegionFactory> getServiceInitiated() {
		return RegionFactory.class;
	}

	@Override
	public RegionFactory initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		final RegionFactory regionFactory = resolveRegionFactory( configurationValues, registry );
		if ( regionFactory instanceof NoCachingRegionFactory ) {
			LOG.noRegionFactory();
		}
		else {
			LOG.regionFactory( regionFactory.getClass().getTypeName() );
		}
		return regionFactory;
	}


	protected RegionFactory resolveRegionFactory(Map<String,Object> configurationValues, ServiceRegistryImplementor registry) {
		final Properties properties = new Properties();
		properties.putAll( configurationValues );

		final Boolean useSecondLevelCache = ConfigurationHelper.getBooleanWrapper(
				AvailableSettings.USE_SECOND_LEVEL_CACHE,
				configurationValues,
				null
		);
		final Boolean useQueryCache = ConfigurationHelper.getBooleanWrapper(
				AvailableSettings.USE_QUERY_CACHE,
				configurationValues,
				null
		);

		// We should immediately return NoCachingRegionFactory if either:
		//		1) both are explicitly FALSE
		//		2) USE_SECOND_LEVEL_CACHE is FALSE and USE_QUERY_CACHE is null
		if ( useSecondLevelCache == FALSE ) {
			if ( useQueryCache == null || useQueryCache == FALSE ) {
				return NoCachingRegionFactory.INSTANCE;
			}
		}

		final Object setting = configurationValues.get( AvailableSettings.CACHE_REGION_FACTORY );

		final StrategySelector selector = registry.requireService( StrategySelector.class );
		final Collection<Class<? extends RegionFactory>> implementors = selector.getRegisteredStrategyImplementors( RegionFactory.class );

		if ( setting == null && implementors.size() != 1 ) {
			// if either is explicitly defined as TRUE we need a RegionFactory
			if ( useSecondLevelCache == TRUE || useQueryCache == TRUE ) {
				throw new CacheException( "Caching was explicitly requested, but no RegionFactory was defined and there is not a single registered RegionFactory" );
			}
		}

		final RegionFactory regionFactory = registry.requireService( StrategySelector.class ).resolveStrategy(
				RegionFactory.class,
				setting,
				(RegionFactory) null,
				new StrategyCreatorRegionFactoryImpl( properties )
		);

		if ( regionFactory != null ) {
			return regionFactory;
		}


		final RegionFactory fallback = getFallback( configurationValues, registry );
		if ( fallback != null ) {
			return fallback;
		}

		if ( implementors.size() == 1 ) {
			final RegionFactory registeredFactory = selector.resolveStrategy( RegionFactory.class, implementors.iterator().next() );
			configurationValues.put( AvailableSettings.CACHE_REGION_FACTORY, registeredFactory );
			configurationValues.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
			return registeredFactory;
		}
		else {
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf(
						"Cannot default RegionFactory based on registered strategies as `%s` RegionFactory strategies were registered",
						implementors
				);
			}
		}

		return NoCachingRegionFactory.INSTANCE;
	}

	protected RegionFactory getFallback(Map<?,?> configurationValues, ServiceRegistryImplementor registry) {
		return null;
	}
}

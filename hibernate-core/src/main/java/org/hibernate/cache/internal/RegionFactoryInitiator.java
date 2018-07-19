/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	public RegionFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final RegionFactory regionFactory = resolveRegionFactory( configurationValues, registry );

		LOG.debugf( "Cache region factory : %s", regionFactory.getClass().getName() );

		return regionFactory;
	}


	@SuppressWarnings({"unchecked", "WeakerAccess"})
	protected RegionFactory resolveRegionFactory(Map configurationValues, ServiceRegistryImplementor registry) {
		final Properties p = new Properties();
		p.putAll( configurationValues );

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
		if ( useSecondLevelCache != null && useSecondLevelCache == FALSE ) {
			if ( useQueryCache == null || useQueryCache == FALSE ) {
				return NoCachingRegionFactory.INSTANCE;
			}
		}

		final Object setting = configurationValues.get( AvailableSettings.CACHE_REGION_FACTORY );

		final StrategySelector selector = registry.getService( StrategySelector.class );
		final Collection<Class<? extends RegionFactory>> implementors = selector.getRegisteredStrategyImplementors( RegionFactory.class );

		if ( setting == null && implementors.size() != 1 ) {
			// if either are explicitly defined as TRUE we need a RegionFactory
			if ( ( useSecondLevelCache != null && useSecondLevelCache == TRUE )
					|| ( useQueryCache != null && useQueryCache == TRUE ) ) {
				throw new CacheException( "Caching was explicitly requested, but no RegionFactory was defined and there is not a single registered RegionFactory" );
			}
		}

		final RegionFactory regionFactory = registry.getService( StrategySelector.class ).resolveStrategy(
				RegionFactory.class,
				setting,
				(RegionFactory) null,
				new StrategyCreatorRegionFactoryImpl( p )
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
			LOG.debugf(
					"Cannot default RegionFactory based on registered strategies as `%s` RegionFactory strategies were registered",
					implementors
			);
		}

		return NoCachingRegionFactory.INSTANCE;
	}

	@SuppressWarnings({"WeakerAccess", "unused"})
	protected RegionFactory getFallback(Map configurationValues, ServiceRegistryImplementor registry) {
		return null;
	}
}

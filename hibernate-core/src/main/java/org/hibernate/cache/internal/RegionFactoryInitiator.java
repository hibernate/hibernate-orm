/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.selector.spi.StrategySelectionException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;

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
	@SuppressWarnings({ "unchecked" })
	public RegionFactory initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Properties p = new Properties();
		if (configurationValues != null) {
			p.putAll( configurationValues );
		}
		
		final boolean useSecondLevelCache = ConfigurationHelper.getBoolean(
				AvailableSettings.USE_SECOND_LEVEL_CACHE,
				configurationValues,
				true
		);
		final boolean useQueryCache = ConfigurationHelper.getBoolean(
				AvailableSettings.USE_QUERY_CACHE,
				configurationValues
		);

		RegionFactory regionFactory = NoCachingRegionFactory.INSTANCE;

		// The cache provider is needed when we either have second-level cache enabled
		// or query cache enabled.  Note that useSecondLevelCache is enabled by default
		if ( useSecondLevelCache || useQueryCache ) {
			final Object strategyReference = configurationValues != null
					? configurationValues.get( AvailableSettings.CACHE_REGION_FACTORY )
					: null;
			regionFactory = resolveRegionFactory(
					registry.getService( StrategySelector.class ),
					strategyReference,
					p
			);
		}

		LOG.debugf( "Cache region factory : %s", regionFactory.getClass().getName() );

		return regionFactory;
	}

	@SuppressWarnings("unchecked")
	private RegionFactory resolveRegionFactory(
			StrategySelector strategySelector,
			Object strategyReference,
			Properties properties) {

		if ( strategyReference == null || RegionFactory.class.isInstance( strategyReference ) ) {
			// nothing to create; just let strategySelector resolve the RegionFactory
			return strategySelector.resolveDefaultableStrategy(
					RegionFactory.class,
					strategyReference,
					NoCachingRegionFactory.INSTANCE
			);
		}

		final Class<? extends RegionFactory> implementationClass;
		if ( Class.class.isInstance( strategyReference ) ) {
			implementationClass = (Class<RegionFactory>) strategyReference;
		}
		else {
			implementationClass = strategySelector.selectStrategyImplementor(
					RegionFactory.class,
					strategyReference.toString()
			);
		}

		try {
			return new StrategyCreatorRegionFactoryImpl( properties ).create( implementationClass );
		}
		catch (Exception e) {
			throw new StrategySelectionException(
					String.format( "Could not instantiate named strategy class [%s]", implementationClass.getName() ),
					e
			);
		}
	}

	/**
	 * Map legacy names unto the new corollary.
	 *
	 * TODO: temporary hack for org.hibernate.cfg.SettingsFactory.createRegionFactory()
	 *
	 * @param name The (possibly legacy) factory name
	 *
	 * @return The factory name to use.
	 */
	public static String mapLegacyNames(final String name) {
		if ( "org.hibernate.cache.EhCacheRegionFactory".equals( name ) ) {
			return "org.hibernate.cache.ehcache.EhCacheRegionFactory";
		}

		if ( "org.hibernate.cache.SingletonEhCacheRegionFactory".equals( name ) ) {
			return "org.hibernate.cache.ehcache.SingletonEhCacheRegionFactory";
		}

		return name;
	}
}

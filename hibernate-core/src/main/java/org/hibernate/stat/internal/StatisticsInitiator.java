/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.internal;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiatorContext;
import org.hibernate.stat.spi.StatisticsFactory;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class StatisticsInitiator implements SessionFactoryServiceInitiator<StatisticsImplementor> {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, StatisticsInitiator.class.getName() );

	public static final StatisticsInitiator INSTANCE = new StatisticsInitiator();

	/**
	 * Names the {@link StatisticsFactory} to use.  Recognizes both a class name as well as an instance of
	 * {@link StatisticsFactory}.
	 */
	public static final String STATS_BUILDER = "hibernate.stats.factory";

	@Override
	public Class<StatisticsImplementor> getServiceInitiated() {
		return StatisticsImplementor.class;
	}

	@Override
	public StatisticsImplementor initiateService(SessionFactoryServiceInitiatorContext context) {
		final Object configValue = context.getServiceRegistry()
				.getService( ConfigurationService.class )
				.getSettings()
				.get( STATS_BUILDER );
		return initiateServiceInternal( context.getSessionFactory(), configValue, context.getServiceRegistry() );
	}

	@Override
	public StatisticsImplementor initiateService(
			SessionFactoryImplementor sessionFactory,
			SessionFactoryOptions sessionFactoryOptions,
			ServiceRegistryImplementor registry) {
		final Object configValue = registry
				.getService( ConfigurationService.class )
				.getSettings()
				.get( STATS_BUILDER );
		return initiateServiceInternal( sessionFactory, configValue, registry );
	}

	private StatisticsImplementor initiateServiceInternal(
			SessionFactoryImplementor sessionFactory,
			Object configValue,
			ServiceRegistryImplementor registry) {

		final StatisticsFactory statisticsFactory;
		if ( configValue == null ) {
			statisticsFactory = null; //We'll use the default
		}
		else if ( StatisticsFactory.class.isInstance( configValue ) ) {
			statisticsFactory = (StatisticsFactory) configValue;
		}
		else {
			// assume it names the factory class
			final ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
			try {
				statisticsFactory = (StatisticsFactory) classLoaderService.classForName( configValue.toString() ).newInstance();
			}
			catch (HibernateException e) {
				throw e;
			}
			catch (Exception e) {
				throw new HibernateException(
						"Unable to instantiate specified StatisticsFactory implementation [" + configValue.toString() + "]",
						e
				);
			}
		}
		final StatisticsImplementor statistics;
		if ( statisticsFactory == null ) {
			// Default:
			statistics = new StatisticsImpl( sessionFactory );
		}
		else {
			statistics = statisticsFactory.buildStatistics( sessionFactory );
		}
		final boolean enabled = sessionFactory.getSettings().isStatisticsEnabled();
		statistics.setStatisticsEnabled( enabled );
		LOG.debugf( "Statistics initialized [enabled=%s]", enabled );
		return statistics;
	}

}

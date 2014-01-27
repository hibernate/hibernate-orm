/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.stat.internal;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
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
	public StatisticsImplementor initiateService(
			SessionFactoryImplementor sessionFactory,
			Configuration configuration,
			ServiceRegistryImplementor registry) {
		final Object configValue = configuration.getProperties().get( STATS_BUILDER );
		return initiateServiceInternal( sessionFactory, configValue, registry );
	}

	@Override
	public StatisticsImplementor initiateService(
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor metadata,
			ServiceRegistryImplementor registry) {
		ConfigurationService configurationService =  registry.getService( ConfigurationService.class );
		final Object configValue = configurationService.getSetting( STATS_BUILDER, null );
		return initiateServiceInternal( sessionFactory, configValue, registry );
	}

	private StatisticsImplementor initiateServiceInternal(
			SessionFactoryImplementor sessionFactory,
			Object configValue,
			ServiceRegistryImplementor registry) {

		StatisticsFactory statisticsFactory;
		if ( configValue == null ) {
			statisticsFactory = DEFAULT_STATS_BUILDER;
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

		StatisticsImplementor statistics = statisticsFactory.buildStatistics( sessionFactory );
		final boolean enabled = sessionFactory.getSettings().isStatisticsEnabled();
		statistics.setStatisticsEnabled( enabled );
		LOG.debugf( "Statistics initialized [enabled=%s]", enabled );
		return statistics;
	}

	private static StatisticsFactory DEFAULT_STATS_BUILDER = new StatisticsFactory() {
		@Override
		public StatisticsImplementor buildStatistics(SessionFactoryImplementor sessionFactory) {
			return new ConcurrentStatisticsImpl( sessionFactory );
		}
	};
}

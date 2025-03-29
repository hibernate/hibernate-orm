/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.stat.internal;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceInitiatorContext;
import org.hibernate.stat.spi.StatisticsFactory;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.jboss.logging.Logger;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.invoke.MethodHandles;

import static org.hibernate.cfg.StatisticsSettings.STATS_BUILDER;

/**
 * @author Steve Ebersole
 */
public class StatisticsInitiator implements SessionFactoryServiceInitiator<StatisticsImplementor> {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, StatisticsInitiator.class.getName() );

	public static final StatisticsInitiator INSTANCE = new StatisticsInitiator();

	@Override
	public Class<StatisticsImplementor> getServiceInitiated() {
		return StatisticsImplementor.class;
	}

	@Override
	public StatisticsImplementor initiateService(SessionFactoryServiceInitiatorContext context) {
		final Object configValue =
				context.getServiceRegistry().requireService( ConfigurationService.class )
						.getSettings().get( STATS_BUILDER );
		final StatisticsFactory statisticsFactory = statisticsFactory( configValue, context.getServiceRegistry() );
		final StatisticsImplementor statistics = statisticsFactory.buildStatistics( context.getSessionFactory() );
		final boolean enabled = context.getSessionFactoryOptions().isStatisticsEnabled();
		statistics.setStatisticsEnabled( enabled );
		LOG.debugf( "Statistics initialized [enabled=%s]", enabled );
		return statistics;
	}

	private static StatisticsFactory statisticsFactory(
			@Nullable Object configValue, ServiceRegistryImplementor registry) {
		if ( configValue == null ) {
			return StatisticsImpl::new; // Use the default implementation
		}
		else if ( configValue instanceof StatisticsFactory factory ) {
			return factory;
		}
		else {
			// assume it names the factory class
			final ClassLoaderService classLoaderService = registry.requireService( ClassLoaderService.class );
			try {
				return (StatisticsFactory) classLoaderService.classForName( configValue.toString() ).newInstance();
			}
			catch (HibernateException e) {
				throw e;
			}
			catch (Exception e) {
				throw new HibernateException(
						"Unable to instantiate specified StatisticsFactory implementation [" + configValue + "]",
						e
				);
			}
		}
	}
}

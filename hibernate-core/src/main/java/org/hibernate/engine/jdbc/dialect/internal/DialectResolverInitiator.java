/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.dialect.internal;

import java.util.Collection;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Standard initiator for the {@link DialectResolver} service
 *
 * @author Steve Ebersole
 */
public class DialectResolverInitiator implements StandardServiceInitiator<DialectResolver> {
	/**
	 * Singleton access
	 */
	public static final DialectResolverInitiator INSTANCE = new DialectResolverInitiator();

	@Override
	public Class<DialectResolver> getServiceInitiated() {
		return DialectResolver.class;
	}

	@Override
	public DialectResolver initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		final DialectResolverSet resolverSet = new DialectResolverSet();

		applyCustomerResolvers( resolverSet, registry, configurationValues );
		resolverSet.addResolver( new StandardDialectResolver() );

		return resolverSet;
	}

	private void applyCustomerResolvers(
			DialectResolverSet resolverSet,
			ServiceRegistryImplementor registry,
			Map<?,?> configurationValues) {
		final String resolverImplNames = (String) configurationValues.get( AvailableSettings.DIALECT_RESOLVERS );

		final ClassLoaderService classLoaderService = registry.requireService( ClassLoaderService.class );
		if ( StringHelper.isNotEmpty( resolverImplNames ) ) {
			for ( String resolverImplName : StringHelper.split( ", \n\r\f\t", resolverImplNames ) ) {
				try {
					final DialectResolver dialectResolver = (DialectResolver)
							classLoaderService.classForName( resolverImplName ).newInstance();
					resolverSet.addResolver( dialectResolver );
				}
				catch (HibernateException e) {
					throw e;
				}
				catch (Exception e) {
					throw new ServiceException( "Unable to instantiate named dialect resolver [" + resolverImplName + "]", e );
				}
			}
		}

		final Collection<DialectResolver> resolvers = classLoaderService.loadJavaServices( DialectResolver.class );
		resolverSet.addDiscoveredResolvers( resolvers );
	}
}

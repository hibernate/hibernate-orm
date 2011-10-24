/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.service.jdbc.dialect.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.jdbc.dialect.spi.DialectResolver;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Standard initiator for the standard {@link DialectResolver} service
 *
 * @author Steve Ebersole
 */
public class DialectResolverInitiator implements BasicServiceInitiator<DialectResolver> {
	public static final DialectResolverInitiator INSTANCE = new DialectResolverInitiator();

	@Override
	public Class<DialectResolver> getServiceInitiated() {
		return DialectResolver.class;
	}

	@Override
	public DialectResolver initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new DialectResolverSet( determineResolvers( configurationValues, registry ) );
	}

	private List<DialectResolver> determineResolvers(Map configurationValues, ServiceRegistryImplementor registry) {
		final List<DialectResolver> resolvers = new ArrayList<DialectResolver>();

		final String resolverImplNames = (String) configurationValues.get( AvailableSettings.DIALECT_RESOLVERS );

		if ( StringHelper.isNotEmpty( resolverImplNames ) ) {
			final ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
			for ( String resolverImplName : StringHelper.split( ", \n\r\f\t", resolverImplNames ) ) {
				try {
					resolvers.add( (DialectResolver) classLoaderService.classForName( resolverImplName ).newInstance() );
				}
				catch (HibernateException e) {
					throw e;
				}
				catch (Exception e) {
					throw new ServiceException( "Unable to instantiate named dialect resolver [" + resolverImplName + "]", e );
				}
			}
		}

		resolvers.add( new StandardDialectResolver() );
		return resolvers;
	}
}

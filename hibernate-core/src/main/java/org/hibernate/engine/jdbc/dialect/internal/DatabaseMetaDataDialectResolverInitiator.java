/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.dialect.internal;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseInfoDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolver;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Initiator for the DatabaseInfoDialectResolver service
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class DatabaseMetaDataDialectResolverInitiator implements StandardServiceInitiator<DatabaseMetaDataDialectResolver> {
	/**
	 * Singleton access
	 */
	public static final DatabaseMetaDataDialectResolverInitiator INSTANCE = new DatabaseMetaDataDialectResolverInitiator();

	@Override
	public Class<DatabaseMetaDataDialectResolver> getServiceInitiated() {
		return DatabaseMetaDataDialectResolver.class;
	}

	@Override
	public DatabaseMetaDataDialectResolver initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final DatabaseMetaDataDialectResolverSet resolver = new DatabaseMetaDataDialectResolverSet();
		applyCustomResolvers( resolver, configurationValues, registry );
		resolver.addResolver(
				new StandardDatabaseMetaDataDialectResolver( StandardDatabaseInfoDialectResolver.INSTANCE ) );
		return resolver;
	}

	private void applyCustomResolvers(
			DatabaseMetaDataDialectResolverSet resolver,
			Map configurationValues,
			ServiceRegistryImplementor registry) {
		final String resolverImplNames = (String) configurationValues.get( AvailableSettings.DIALECT_RESOLVERS );

		if ( StringHelper.isNotEmpty( resolverImplNames ) ) {
			final ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
			for ( String resolverImplName : StringHelper.split( ", \n\r\f\t", resolverImplNames ) ) {
				try {
					final Object obj = classLoaderService.classForName( resolverImplName ).newInstance();
					if (DatabaseMetaDataDialectResolver.class.isInstance( obj )) {
						resolver.addResolver( (DatabaseMetaDataDialectResolver) obj );
					}
					else if (DatabaseInfoDialectResolver.class.isInstance( obj )) {
						resolver.addResolver(
								new StandardDatabaseMetaDataDialectResolver( (DatabaseInfoDialectResolver) obj ) );
					}
					else {
						throw new ServiceException( "The custom resolver " + resolverImplName
								+ " must implement DatabaseMetaDataDialectResolver or DatabaseInfoDialectResolver!" );
					}
				}
				catch (HibernateException e) {
					throw e;
				}
				catch (Exception e) {
					throw new ServiceException( "Unable to instantiate named dialect resolver [" + resolverImplName + "]", e );
				}
			}
		}
	}
}

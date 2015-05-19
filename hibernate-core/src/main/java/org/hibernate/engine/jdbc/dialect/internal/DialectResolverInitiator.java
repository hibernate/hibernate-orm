/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.dialect.internal;

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
 * Standard initiator for the standard {@link DialectResolver} service
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("deprecation")
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
	public DialectResolver initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final DialectResolverSet resolver = new DialectResolverSet();

		applyCustomerResolvers( resolver, registry, configurationValues );
		resolver.addResolver(StandardDialectResolver.INSTANCE );

		return resolver;
	}

	private void applyCustomerResolvers(
			DialectResolverSet resolver,
			ServiceRegistryImplementor registry,
			Map configurationValues) {
		final String resolverImplNames = (String) configurationValues.get( AvailableSettings.DIALECT_RESOLVERS );

		if ( StringHelper.isNotEmpty( resolverImplNames ) ) {
			final ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
			for ( String resolverImplName : StringHelper.split( ", \n\r\f\t", resolverImplNames ) ) {
				try {
					resolver.addResolver(
							(DialectResolver) classLoaderService.classForName( resolverImplName ).newInstance()
					);
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

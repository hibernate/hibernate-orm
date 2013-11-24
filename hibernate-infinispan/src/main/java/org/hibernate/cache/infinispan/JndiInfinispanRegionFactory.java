/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.hibernate.cache.infinispan;

import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hibernate.cache.CacheException;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.internal.util.jndi.JndiHelper;

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * A {@link org.hibernate.cache.spi.RegionFactory} for <a href="http://www.jboss.org/infinispan">Infinispan</a>-backed cache
 * regions that finds its cache manager in JNDI rather than creating one itself.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class JndiInfinispanRegionFactory extends InfinispanRegionFactory {

	private static final Log log = LogFactory.getLog( JndiInfinispanRegionFactory.class );

	/**
	 * Specifies the JNDI name under which the {@link EmbeddedCacheManager} to use is bound.
	 * There is no default value -- the user must specify the property.
	 */
	public static final String CACHE_MANAGER_RESOURCE_PROP = "hibernate.cache.infinispan.cachemanager";

	/**
	 * Constructs a JndiInfinispanRegionFactory
	 */
	@SuppressWarnings("UnusedDeclaration")
	public JndiInfinispanRegionFactory() {
		super();
	}

	/**
	 * Constructs a JndiInfinispanRegionFactory
	 *
	 * @param props Any properties to apply (not used).
	 */
	@SuppressWarnings("UnusedDeclaration")
	public JndiInfinispanRegionFactory(Properties props) {
		super( props );
	}

	@Override
	protected EmbeddedCacheManager createCacheManager(Properties properties) throws CacheException {
		final String name = ConfigurationHelper.getString( CACHE_MANAGER_RESOURCE_PROP, properties, null );
		if ( name == null ) {
			throw new CacheException( "Configuration property " + CACHE_MANAGER_RESOURCE_PROP + " not set" );
		}
		return locateCacheManager( name, JndiHelper.extractJndiProperties( properties ) );
	}

	private EmbeddedCacheManager locateCacheManager(String jndiNamespace, Properties jndiProperties) {
		Context ctx = null;
		try {
			ctx = new InitialContext( jndiProperties );
			return (EmbeddedCacheManager) ctx.lookup( jndiNamespace );
		}
		catch (NamingException ne) {
			final String msg = "Unable to retrieve CacheManager from JNDI [" + jndiNamespace + "]";
			log.info( msg, ne );
			throw new CacheException( msg );
		}
		finally {
			if ( ctx != null ) {
				try {
					ctx.close();
				}
				catch (NamingException ne) {
					log.info( "Unable to release initial context", ne );
				}
			}
		}
	}

	@Override
	public void stop() {
		// Do not attempt to stop a cache manager because it wasn't created by this region factory.
	}
}

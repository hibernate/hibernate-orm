/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.cache;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.util.NamingHelper;
import org.hibernate.util.StringHelper;

/**
 * Support for CacheProvider implementations which are backed by caches bound
 * into JNDI namespace.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJndiBoundCacheProvider implements CacheProvider {

	private static final Logger log = LoggerFactory.getLogger( AbstractJndiBoundCacheProvider.class );
	private Object cache;

	protected void prepare(Properties properties) {
		// Do nothing; subclasses may override.
	}

	protected void release() {
		// Do nothing; subclasses may override.
	}

	/**
	 * Callback to perform any necessary initialization of the underlying cache implementation during SessionFactory
	 * construction.
	 *
	 * @param properties current configuration settings.
	 */
	public final void start(Properties properties) throws CacheException {
		String jndiNamespace = properties.getProperty( Environment.CACHE_NAMESPACE );
		if ( StringHelper.isEmpty( jndiNamespace ) ) {
			throw new CacheException( "No JNDI namespace specified for cache" );
		}
		cache = locateCache( jndiNamespace, NamingHelper.getJndiProperties( properties ) );
		prepare( properties );
	}

	/**
	 * Callback to perform any necessary cleanup of the underlying cache
	 * implementation during SessionFactory.close().
	 */
	public final void stop() {
		release();
		cache = null;
	}

	private Object locateCache(String jndiNamespace, Properties jndiProperties) {

		Context ctx = null;
		try {
			ctx = new InitialContext( jndiProperties );
			return ctx.lookup( jndiNamespace );
		}
		catch (NamingException ne) {
			String msg = "Unable to retreive Cache from JNDI [" + jndiNamespace + "]";
			log.info( msg, ne );
			throw new CacheException( msg );
		}
		finally {
			if ( ctx != null ) {
				try {
					ctx.close();
				}
				catch( NamingException ne ) {
					log.info( "Unable to release initial context", ne );
				}
			}
		}
	}
	
	public Object getCache() {
		return cache;
	}
}

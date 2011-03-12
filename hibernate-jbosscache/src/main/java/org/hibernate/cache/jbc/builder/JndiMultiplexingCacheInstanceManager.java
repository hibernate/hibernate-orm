/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
 */
package org.hibernate.cache.jbc.builder;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hibernate.cache.CacheException;
import org.hibernate.cfg.Settings;
import org.hibernate.util.NamingHelper;
import org.hibernate.util.PropertiesHelper;
import org.jboss.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A {@link MultiplexingCacheInstanceManager} that finds its cache factory
 * in JNDI rather than creating one itself.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class JndiMultiplexingCacheInstanceManager extends MultiplexingCacheInstanceManager {
    
    private static final Logger log = LoggerFactory.getLogger(JndiMultiplexingCacheInstanceManager.class);
    
    /**
     * Specifies the JNDI name under which the {@link CacheManager} to use is bound.
     * There is no default value -- the user must specify the property.
     */
    public static final String CACHE_FACTORY_RESOURCE_PROP = "hibernate.cache.region.jbc2.cachefactory";

    /**
     * Create a new JndiMultiplexingCacheInstanceManager.
     */
    public JndiMultiplexingCacheInstanceManager() {
        super();
    }

    @Override
    public void start(Settings settings, Properties properties) throws CacheException {
        
        String name = PropertiesHelper.getString(CACHE_FACTORY_RESOURCE_PROP, properties, null);
        if (name == null)
            throw new CacheException("Configuration property " + CACHE_FACTORY_RESOURCE_PROP + " not set");
        
        CacheManager cf = locateCacheFactory( name, NamingHelper.getJndiProperties( properties ) );
        setCacheFactory( cf );        
        
        super.start(settings, properties);
    }

    private CacheManager locateCacheFactory(String jndiNamespace, Properties jndiProperties) {

        Context ctx = null;
        try {
            ctx = new InitialContext( jndiProperties );
            return (CacheManager) ctx.lookup( jndiNamespace );
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

    
}

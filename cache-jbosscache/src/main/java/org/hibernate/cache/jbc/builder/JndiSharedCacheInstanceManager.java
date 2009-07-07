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
import org.jboss.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SharedCacheInstanceManager} that finds the shared cache in JNDI 
 * rather than instantiating one from an XML config file.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class JndiSharedCacheInstanceManager extends SharedCacheInstanceManager {
    
    private static final Logger log = LoggerFactory.getLogger(JndiSharedCacheInstanceManager.class);

    /**
     * Specifies the JNDI name under which the {@link Cache} to use is bound.
     * <p>
     * Note that although this configuration property has the same name as that by
     * in {@link SharedCacheInstanceManager#CACHE_RESOURCE_PROP the superclass}, 
     * the meaning here is different. Note also that in this class' usage
     * of the property, there is no default value -- the user must specify
     * the property.
     */
    public static final String CACHE_RESOURCE_PROP = "hibernate.cache.region.jbc2.cfg.shared";
    
    /**
     * Create a new JndiSharedCacheInstanceManager.
     * 
     */
    public JndiSharedCacheInstanceManager() {
        super();
    }

    @Override
    protected Cache createSharedCache(Settings settings, Properties properties) {
        
        String name = PropertiesHelper.getString(CACHE_RESOURCE_PROP, properties, null);
        if (name == null)
            throw new CacheException("Configuration property " + CACHE_RESOURCE_PROP + " not set");
        
        return locateCache( name, NamingHelper.getJndiProperties( properties ) );
    }

    /**
     * No-op; we don't own the cache so we shouldn't stop it.
     */
    @Override
    protected void stopSharedCache(Cache cache) {
        // no-op. We don't own the cache so we shouldn't stop it.
    }

    private Cache locateCache(String jndiNamespace, Properties jndiProperties) {

        Context ctx = null;
        try {
            ctx = new InitialContext( jndiProperties );
            return (Cache) ctx.lookup( jndiNamespace );
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

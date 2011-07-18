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
package org.hibernate.cache.ehcache.internal.regions;

import java.util.Properties;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import org.jboss.logging.Logger;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.EhCacheMessageLogger;
import org.hibernate.cache.ehcache.internal.nonstop.HibernateNonstopCacheExceptionHandler;
import org.hibernate.cache.ehcache.internal.strategy.EhcacheAccessStrategyFactory;
import org.hibernate.cache.spi.GeneralDataRegion;

/**
 * An Ehcache specific GeneralDataRegion.
 * <p/>
 * GeneralDataRegion instances are used for both the timestamps and query caches.
 *
 * @author Chris Dennis
 * @author Greg Luck
 * @author Emmanuel Bernard
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
abstract class EhcacheGeneralDataRegion extends EhcacheDataRegion implements GeneralDataRegion {

    private static final EhCacheMessageLogger LOG = Logger.getMessageLogger(
            EhCacheMessageLogger.class,
            EhcacheGeneralDataRegion.class.getName()
    );

    /**
     * Creates an EhcacheGeneralDataRegion using the given Ehcache instance as a backing.
     */
    public EhcacheGeneralDataRegion(EhcacheAccessStrategyFactory accessStrategyFactory, Ehcache cache, Properties properties) {
        super( accessStrategyFactory, cache, properties );
    }

    /**
     * {@inheritDoc}
     */
    public Object get(Object key) throws CacheException {
        try {
            LOG.debugf( "key: %s", key );
            if ( key == null ) {
                return null;
            }
            else {
                Element element = cache.get( key );
                if ( element == null ) {
                    LOG.debugf( "Element for key %s is null", key );
                    return null;
                }
                else {
                    return element.getObjectValue();
                }
            }
        }
        catch ( net.sf.ehcache.CacheException e ) {
            if ( e instanceof NonStopCacheException ) {
                HibernateNonstopCacheExceptionHandler.getInstance()
                        .handleNonstopCacheException( (NonStopCacheException) e );
                return null;
            }
            else {
                throw new CacheException( e );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void put(Object key, Object value) throws CacheException {
        LOG.debugf( "key: %s value: %s", key, value );
        try {
            Element element = new Element( key, value );
            cache.put( element );
        }
        catch ( IllegalArgumentException e ) {
            throw new CacheException( e );
        }
        catch ( IllegalStateException e ) {
            throw new CacheException( e );
        }
        catch ( net.sf.ehcache.CacheException e ) {
            if ( e instanceof NonStopCacheException ) {
                HibernateNonstopCacheExceptionHandler.getInstance()
                        .handleNonstopCacheException( (NonStopCacheException) e );
            }
            else {
                throw new CacheException( e );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void evict(Object key) throws CacheException {
        try {
            cache.remove( key );
        }
        catch ( ClassCastException e ) {
            throw new CacheException( e );
        }
        catch ( IllegalStateException e ) {
            throw new CacheException( e );
        }
        catch ( net.sf.ehcache.CacheException e ) {
            if ( e instanceof NonStopCacheException ) {
                HibernateNonstopCacheExceptionHandler.getInstance()
                        .handleNonstopCacheException( (NonStopCacheException) e );
            }
            else {
                throw new CacheException( e );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void evictAll() throws CacheException {
        try {
            cache.removeAll();
        }
        catch ( IllegalStateException e ) {
            throw new CacheException( e );
        }
        catch ( net.sf.ehcache.CacheException e ) {
            if ( e instanceof NonStopCacheException ) {
                HibernateNonstopCacheExceptionHandler.getInstance()
                        .handleNonstopCacheException( (NonStopCacheException) e );
            }
            else {
                throw new CacheException( e );
            }
        }
    }
}

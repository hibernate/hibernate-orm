/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.cache.ehcache.internal.regions;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import net.sf.ehcache.util.Timestamper;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.AbstractEhcacheRegionFactory;
import org.hibernate.cache.ehcache.EhCacheMessageLogger;
import org.hibernate.cache.ehcache.internal.nonstop.HibernateNonstopCacheExceptionHandler;
import org.hibernate.cache.ehcache.internal.strategy.EhcacheAccessStrategyFactory;
import org.hibernate.cache.spi.Region;

import org.jboss.logging.Logger;

import static javafx.scene.input.KeyCode.L;

/**
 * An Ehcache specific data region implementation.
 * <p/>
 * This class is the ultimate superclass for all Ehcache Hibernate cache regions.
 *
 * @author Chris Dennis
 * @author Greg Luck
 * @author Emmanuel Bernard
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
public abstract class EhcacheDataRegion implements Region,
																	Externalizable {
	private static final EhCacheMessageLogger LOG = Logger.getMessageLogger(
			EhCacheMessageLogger.class,
			EhcacheDataRegion.class.getName()
	);
	private static final String CACHE_LOCK_TIMEOUT_PROPERTY = "net.sf.ehcache.hibernate.cache_lock_timeout";
	private static final int DEFAULT_CACHE_LOCK_TIMEOUT = 60000;
	private static final Logger log = Logger.getLogger(EhcacheDataRegion.class);

	protected Ehcache cache;
	private String cacheName;
	private AbstractEhcacheRegionFactory regionFactory;
	private EhcacheAccessStrategyFactory accessStrategyFactory;
	private int cacheLockTimeout;

	public EhcacheDataRegion() {}

	/**
	 * Create a Hibernate data region backed by the given Ehcache instance.
	 */
	EhcacheDataRegion(AbstractEhcacheRegionFactory regionFactory, EhcacheAccessStrategyFactory accessStrategyFactory, Ehcache cache, Properties properties) {
		this.regionFactory = regionFactory;
		this.accessStrategyFactory = accessStrategyFactory;
		this.cache = cache;
		this.cacheName = cache.getName();
		final String timeout = properties.getProperty(
				CACHE_LOCK_TIMEOUT_PROPERTY,
				Integer.toString( DEFAULT_CACHE_LOCK_TIMEOUT )
		);
		this.cacheLockTimeout = Timestamper.ONE_MS * Integer.decode( timeout );
	}

	/**
	 * Ehcache instance backing this Hibernate data region.
	 */
	protected Ehcache getCache() {
		return cache;
	}

	/**
	 * The {@link org.hibernate.cache.ehcache.internal.strategy.EhcacheAccessStrategyFactory} used for creating
	 * various access strategies
	 */
	protected EhcacheAccessStrategyFactory getAccessStrategyFactory() {
		return accessStrategyFactory;
	}

	/**
	 * Return the Ehcache instance backing this Hibernate data region.
	 *
	 * @return The underlying ehcache cache
	 */
	public Ehcache getEhcache() {
		return getCache();
	}

	@Override
	public String getName() {
		return cacheName;
	}

	@Override
	public void destroy() throws CacheException {
		try {
			getCache().getCacheManager().removeCache( getCache().getName() );
		}
		catch (IllegalStateException e) {
			//When Spring and Hibernate are both involved this will happen in normal shutdown operation.
			//Do not throw an exception, simply log this one.
			LOG.debug( "This can happen if multiple frameworks both try to shutdown ehcache", e );
		}
		catch (net.sf.ehcache.CacheException e) {
			if ( e instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) e );
			}
			else {
				throw new CacheException( e );
			}
		}
	}

	@Override
	public long getSizeInMemory() {
		try {
			return getCache().calculateInMemorySize();
		}
		catch (Throwable t) {
			if ( t instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) t );
			}
			return -1;
		}
	}

	@Override
	public long getElementCountInMemory() {
		try {
			return getCache().getMemoryStoreSize();
		}
		catch (net.sf.ehcache.CacheException ce) {
			if ( ce instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) ce );
				return -1;
			}
			else {
				throw new CacheException( ce );
			}
		}
	}

	@Override
	public long getElementCountOnDisk() {
		try {
			return getCache().getDiskStoreSize();
		}
		catch (net.sf.ehcache.CacheException ce) {
			if ( ce instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) ce );
				return -1;
			}
			else {
				throw new CacheException( ce );
			}
		}
	}

	@Override
	public Map toMap() {
		try {
			final Map<Object, Object> result = new HashMap<Object, Object>();
			for ( Object key : getCache().getKeys() ) {
				result.put( key, getCache().get( key ).getObjectValue() );
			}
			return result;
		}
		catch (Exception e) {
			if ( e instanceof NonStopCacheException ) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException( (NonStopCacheException) e );
				return Collections.emptyMap();
			}
			else {
				throw new CacheException( e );
			}
		}
	}

	@Override
	public long nextTimestamp() {
		return Timestamper.next();
	}

	@Override
	public int getTimeout() {
		return cacheLockTimeout;
	}

	@Override
	public boolean contains(Object key) {
		return getCache().isKeyInCache( key );
	}

//	void writeObject(ObjectOutputStream oos) throws IOException, ClassNotFoundException {
//		oos.defaultWriteObject();
//	}
//
//	void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
//		ois.defaultReadObject();
//		log.trace("readObject in EhcacheDataRegion thank god");
//		cache = regionFactory.getManager().getCache(cacheName);
//	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(regionFactory);
		out.writeObject(accessStrategyFactory);
		out.writeInt(cacheLockTimeout);
		out.writeUTF(cacheName);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		regionFactory = (AbstractEhcacheRegionFactory) in.readObject();
		accessStrategyFactory = (EhcacheAccessStrategyFactory) in.readObject();
		cacheLockTimeout = in.readInt();
		cacheName = in.readUTF();
		cache = regionFactory.getManager().getCache(cacheName);
	}

	public void setCache(Ehcache cache) {
		this.cache = cache;
	}
}

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
package org.hibernate.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.optimistic.DataVersion;
import org.jboss.cache.config.Option;
import org.jboss.cache.lock.TimeoutException;

/**
 * Represents a particular region within the given JBossCache TreeCache
 * utilizing TreeCache's optimistic locking capabilities.
 *
 * @see OptimisticTreeCacheProvider for more details
 *
 * @author Steve Ebersole
 */
public class OptimisticTreeCache implements OptimisticCache, TransactionAwareCache {

	// todo : eventually merge this with TreeCache and just add optional opt-lock support there.

	private static final Logger log = LoggerFactory.getLogger( OptimisticTreeCache.class);

	private static final String ITEM = "item";

	private org.jboss.cache.TreeCache cache;
	private final String regionName;
	private final Fqn regionFqn;
	private OptimisticCacheSource source;

	public OptimisticTreeCache(org.jboss.cache.TreeCache cache, String regionName)
	throws CacheException {
		this.cache = cache;
		this.regionName = regionName;
		this.regionFqn = Fqn.fromString( regionName.replace( '.', '/' ) );
	}


	// OptimisticCache impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void setSource(OptimisticCacheSource source) {
		this.source = source;
	}

	public void writeInsert(Object key, Object value, Object currentVersion) {
		writeUpdate( key, value, currentVersion, null );
	}

	public void writeUpdate(Object key, Object value, Object currentVersion, Object previousVersion) {
		try {
			Option option = new Option();
			DataVersion dv = ( source != null && source.isVersioned() )
			                 ? new DataVersionAdapter( currentVersion, previousVersion, source.getVersionComparator(), source.toString() )
			                 : NonLockingDataVersion.INSTANCE;
			option.setDataVersion( dv );
			cache.put( new Fqn( regionFqn, key ), ITEM, value, option );
		}
		catch ( Exception e ) {
			throw new CacheException( e );
		}
	}

	public void writeLoad(Object key, Object value, Object currentVersion) {
		try {
			Option option = new Option();
			option.setFailSilently( true );
			option.setDataVersion( NonLockingDataVersion.INSTANCE );
			cache.remove( new Fqn( regionFqn, key ), "ITEM", option );

			option = new Option();
			option.setFailSilently( true );
			DataVersion dv = ( source != null && source.isVersioned() )
			                 ? new DataVersionAdapter( currentVersion, currentVersion, source.getVersionComparator(), source.toString() )
			                 : NonLockingDataVersion.INSTANCE;
			option.setDataVersion( dv );
			cache.put( new Fqn( regionFqn, key ), ITEM, value, option );
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}


	// Cache impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Object get(Object key) throws CacheException {
		try {
			Option option = new Option();
			option.setFailSilently( true );
//			option.setDataVersion( NonLockingDataVersion.INSTANCE );
			return cache.get( new Fqn( regionFqn, key ), ITEM, option );
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public Object read(Object key) throws CacheException {
		try {
			return cache.get( new Fqn( regionFqn, key ), ITEM );
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public void update(Object key, Object value) throws CacheException {
		try {
			Option option = new Option();
			option.setDataVersion( NonLockingDataVersion.INSTANCE );
			cache.put( new Fqn( regionFqn, key ), ITEM, value, option );
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public void put(Object key, Object value) throws CacheException {
		try {
			log.trace( "performing put() into region [" + regionName + "]" );
			// do the put outside the scope of the JTA txn
			Option option = new Option();
			option.setFailSilently( true );
			option.setDataVersion( NonLockingDataVersion.INSTANCE );
			cache.put( new Fqn( regionFqn, key ), ITEM, value, option );
		}
		catch (TimeoutException te) {
			//ignore!
			log.debug("ignoring write lock acquisition failure");
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public void remove(Object key) throws CacheException {
		try {
			// tree cache in optimistic mode seems to have as very difficult
			// time with remove calls on non-existent nodes (NPEs)...
			if ( cache.get( new Fqn( regionFqn, key ), ITEM ) != null ) {
				Option option = new Option();
				option.setDataVersion( NonLockingDataVersion.INSTANCE );
				cache.remove( new Fqn( regionFqn, key ), option );
			}
			else {
				log.trace( "skipping remove() call as the underlying node did not seem to exist" );
			}
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public void clear() throws CacheException {
		try {
			Option option = new Option();
			option.setDataVersion( NonLockingDataVersion.INSTANCE );
			cache.remove( regionFqn, option );
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public void destroy() throws CacheException {
		try {
			Option option = new Option();
			option.setCacheModeLocal( true );
			option.setFailSilently( true );
			option.setDataVersion( NonLockingDataVersion.INSTANCE );
			cache.remove( regionFqn, option );
		}
		catch( Exception e ) {
			throw new CacheException( e );
		}
	}

	public void lock(Object key) throws CacheException {
		throw new UnsupportedOperationException( "TreeCache is a fully transactional cache" + regionName );
	}

	public void unlock(Object key) throws CacheException {
		throw new UnsupportedOperationException( "TreeCache is a fully transactional cache: " + regionName );
	}

	public long nextTimestamp() {
		return System.currentTimeMillis() / 100;
	}

	public int getTimeout() {
		return 600; //60 seconds
	}

	public String getRegionName() {
		return regionName;
	}

	public long getSizeInMemory() {
		return -1;
	}

	public long getElementCountInMemory() {
		try {
			Set children = cache.getChildrenNames( regionFqn );
			return children == null ? 0 : children.size();
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public long getElementCountOnDisk() {
		return 0;
	}

	public Map toMap() {
		try {
			Map result = new HashMap();
			Set childrenNames = cache.getChildrenNames( regionFqn );
			if (childrenNames != null) {
				Iterator iter = childrenNames.iterator();
				while ( iter.hasNext() ) {
					Object key = iter.next();
					result.put(
							key,
					        cache.get( new Fqn( regionFqn, key ), ITEM )
						);
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public String toString() {
		return "OptimisticTreeCache(" + regionName + ')';
	}

	public static class DataVersionAdapter implements DataVersion {
		private final Object currentVersion;
		private final Object previousVersion;
		private final Comparator versionComparator;
		private final String sourceIdentifer;

		public DataVersionAdapter(Object currentVersion, Object previousVersion, Comparator versionComparator, String sourceIdentifer) {
			this.currentVersion = currentVersion;
			this.previousVersion = previousVersion;
			this.versionComparator = versionComparator;
			this.sourceIdentifer = sourceIdentifer;
			log.trace( "created " + this );
		}

		/**
		 * newerThan() call is dispatched against the DataVersion currently
		 * associated with the node; the passed dataVersion param is the
		 * DataVersion associated with the data we are trying to put into
		 * the node.
		 * <p/>
		 * we are expected to return true in the case where we (the current
		 * node DataVersion) are newer that then incoming value.  Returning
		 * true here essentially means that a optimistic lock failure has
		 * occured (because conversely, the value we are trying to put into
		 * the node is "older than" the value already there...)
		 */
		public boolean newerThan(DataVersion dataVersion) {
			log.trace( "checking [" + this + "] against [" + dataVersion + "]" );
			if ( dataVersion instanceof CircumventChecksDataVersion ) {
				log.trace( "skipping lock checks..." );
				return false;
			}
			else if ( dataVersion instanceof NonLockingDataVersion ) {
				// can happen because of the multiple ways Cache.remove()
				// can be invoked :(
				log.trace( "skipping lock checks..." );
				return false;
			}
			DataVersionAdapter other = ( DataVersionAdapter ) dataVersion;
			if ( other.previousVersion == null ) {
				log.warn( "Unexpected optimistic lock check on inserting data" );
				// work around the "feature" where tree cache is validating the
				// inserted node during the next transaction.  no idea...
				if ( this == dataVersion ) {
					log.trace( "skipping lock checks due to same DV instance" );
					return false;
				}
			}
			return versionComparator.compare( currentVersion, other.previousVersion ) >= 1;
		}

		public String toString() {
			return super.toString() + " [current=" + currentVersion + ", previous=" + previousVersion + ", src=" + sourceIdentifer + "]";
		}
	}

	/**
	 * Used in regions where no locking should ever occur.  This includes query-caches,
	 * update-timestamps caches, collection caches, and entity caches where the entity
	 * is not versioned.
	 */
	public static class NonLockingDataVersion implements DataVersion {
		public static final DataVersion INSTANCE = new NonLockingDataVersion();
		public boolean newerThan(DataVersion dataVersion) {
			log.trace( "non locking lock check...");
			return false;
		}
	}

	/**
	 * Used to signal to a DataVersionAdapter to simply not perform any checks.  This
	 * is currently needed for proper handling of remove() calls for entity cache regions
	 * (we do not know the version info...).
	 */
	public static class CircumventChecksDataVersion implements DataVersion {
		public static final DataVersion INSTANCE = new CircumventChecksDataVersion();
		public boolean newerThan(DataVersion dataVersion) {
			throw new CacheException( "optimistic locking checks should never happen on CircumventChecksDataVersion" );
		}
	}
}

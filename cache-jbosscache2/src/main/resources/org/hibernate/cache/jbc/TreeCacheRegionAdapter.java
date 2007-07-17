package org.hibernate.cache.impl.jbc;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;

import org.jboss.cache.Fqn;
import org.jboss.cache.Cache;
import org.jboss.cache.Node;
import org.jboss.cache.config.Option;

import org.hibernate.cache.Region;
import org.hibernate.cache.CacheException;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class TreeCacheRegionAdapter implements Region {
	private static final String ITEM = "item";

	protected final Node jbcNode;
	protected final String regionName;

	public TreeCacheRegionAdapter(Cache jbcCache, String regionName) {
		this.regionName = regionName;
		Fqn fqn = Fqn.fromString( regionName.replace( '.', '/' ) );
		this.jbcNode = jbcCache.getRoot().addChild( fqn );
	}

	public String getName() {
		return regionName;
	}

	public void destroy() throws CacheException {
		try {
			// NOTE : this is being used from the process of shutting down a
			// SessionFactory.  Specific things to consider:
			// 		(1) this clearing of the region should not propogate to
			// 			other nodes on the cluster (if any); this is the
			//			cache-mode-local option bit...
			//		(2) really just trying a best effort to cleanup after
			// 			ourselves; lock failures, etc are not critical here;
			//			this is the fail-silently option bit...
			Option option = new Option();
			option.setCacheModeLocal( true );
			option.setFailSilently( true );
			jbcNode.
			jbcTreeCache.remove( regionFqn, option );
		}
		catch( Exception e ) {
			throw new CacheException( e );
		}
	}

	public long getSizeInMemory() {
		// not supported
		return -1;
	}

	public long getElementCountInMemory() {
		try {
			Set children = jbcTreeCache.getChildrenNames( regionFqn );
			return children == null ? 0 : children.size();
		}
		catch ( Exception e ) {
			throw new CacheException( e );
		}
	}

	public long getElementCountOnDisk() {
		return -1;
	}

	public Map toMap() {
		try {
			Map result = new HashMap();
			Set childrenNames = jbcTreeCache.getChildrenNames( regionFqn );
			if (childrenNames != null) {
				Iterator iter = childrenNames.iterator();
				while ( iter.hasNext() ) {
					Object key = iter.next();
					result.put(
							key,
					        jbcTreeCache.get( new Fqn( regionFqn, key ), ITEM )
						);
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public long nextTimestamp() {
		return System.currentTimeMillis() / 100;
	}

	public int getTimeout() {
		return 600; //60 seconds
	}
}

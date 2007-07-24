/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Steve Ebersole
 */
package org.hibernate.cache.jbc2;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.config.Option;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.Region;

/**
 * General support for writing {@link Region} implementations for
 *
 *
 * @author Steve Ebersole
 */
public abstract class BasicRegionAdapter implements Region {
	public static final String ITEM = "item";

	protected final Cache jbcCache;
	protected final String regionName;
	protected final Fqn regionFqn;

	public BasicRegionAdapter(Cache jbcCache, String regionName) {
		this.jbcCache = jbcCache;
		this.regionName = regionName;
		this.regionFqn = Fqn.fromString( regionName.replace( '.', '/' ) );
		activateLocalClusterNode();
	}

	private void activateLocalClusterNode() {
		org.jboss.cache.Region jbcRegion = jbcCache.getRegion( regionFqn, true );
		if ( jbcRegion.isActive() ) {
			return;
		}
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if ( classLoader == null ) {
			classLoader = getClass().getClassLoader();
		}
		jbcRegion.registerContextClassLoader( classLoader );
		jbcRegion.activate();
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
			jbcCache.getInvocationContext().setOptionOverrides( option );
			jbcCache.removeNode( regionFqn );
			deactivateLocalNode();
		}
		catch( Exception e ) {
			throw new CacheException( e );
		}
	}

	private void deactivateLocalNode() {
		org.jboss.cache.Region jbcRegion = jbcCache.getRegion( regionFqn, false );
		if ( jbcRegion != null && jbcRegion.isActive() ) {
			jbcRegion.deactivate();
			jbcRegion.unregisterContextClassLoader();
		}
	}

	public long getSizeInMemory() {
		// not supported
		return -1;
	}

	public long getElementCountInMemory() {
		try {
			Set children = jbcCache.getRoot().getChild( regionFqn ).getChildrenNames();
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
			Set childrenNames = jbcCache.getRoot().getChild( regionFqn ).getChildrenNames();
			if (childrenNames != null) {
				for ( Object childName : childrenNames ) {
					result.put( childName, jbcCache.get( new Fqn( regionFqn, childName ), ITEM ) );
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

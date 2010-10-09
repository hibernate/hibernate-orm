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

/**
 * A simple in-memory Hashtable-based cache impl.
 * 
 * @author Gavin King
 */
public class HashtableCacheProvider implements CacheProvider {

	public Cache buildCache(String regionName, Properties properties) throws CacheException {
		return new HashtableCache( regionName );
	}

	public long nextTimestamp() {
		return Timestamper.next();
	}

	/**
	 * Callback to perform any necessary initialization of the underlying cache implementation
	 * during SessionFactory construction.
	 *
	 * @param properties current configuration settings.
	 */
	public void start(Properties properties) throws CacheException {
	}

	/**
	 * Callback to perform any necessary cleanup of the underlying cache implementation
	 * during SessionFactory.close().
	 */
	public void stop() {
	}

	public boolean isMinimalPutsEnabledByDefault() {
		return false;
	}

}


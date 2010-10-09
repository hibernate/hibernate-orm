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
 * Implementation of NoCacheProvider.
 *
 * @author Steve Ebersole
 */
public class NoCacheProvider implements CacheProvider {
	/**
	 * Configure the cache
	 *
	 * @param regionName the name of the cache region
	 * @param properties configuration settings
	 *
	 * @throws CacheException
	 */
	public Cache buildCache(String regionName, Properties properties) throws CacheException {
		throw new NoCachingEnabledException();
	}

	/**
	 * Generate a timestamp
	 */
	public long nextTimestamp() {
		// This, is used by SessionFactoryImpl to hand to the generated SessionImpl;
		// was the only reason I could see that we cannot just use null as
		// Settings.cacheProvider
		return System.currentTimeMillis() / 100;
	}

	/**
	 * Callback to perform any necessary initialization of the underlying cache implementation during SessionFactory
	 * construction.
	 *
	 * @param properties current configuration settings.
	 */
	public void start(Properties properties) throws CacheException {
		// this is called by SessionFactory irregardless; we just disregard here;
		// could also add a check to SessionFactory to only conditionally call start
	}

	/**
	 * Callback to perform any necessary cleanup of the underlying cache implementation during SessionFactory.close().
	 */
	public void stop() {
		// this is called by SessionFactory irregardless; we just disregard here;
		// could also add a check to SessionFactory to only conditionally call stop
	}

	public boolean isMinimalPutsEnabledByDefault() {
		// this is called from SettingsFactory irregardless; trivial to simply disregard
		return false;
	}

}

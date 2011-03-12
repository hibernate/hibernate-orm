/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.impl;

import java.util.Properties;

import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.TimestampsRegion;
import org.hibernate.cache.NoCachingEnabledException;
import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cfg.Settings;

/**
 * Factory used if no caching enabled in config...
 *
 * @author Steve Ebersole
 */
public class NoCachingRegionFactory implements RegionFactory {


	public NoCachingRegionFactory(Properties properties) {
	}

	public void start(Settings settings, Properties properties) throws CacheException {
	}

	public void stop() {
	}

	public boolean isMinimalPutsEnabledByDefault() {
		return false;
	}

	public AccessType getDefaultAccessType() {
		return null;
	}

	public long nextTimestamp() {
		return System.currentTimeMillis() / 100;
	}

	public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException {
		throw new NoCachingEnabledException();
	}

	public CollectionRegion buildCollectionRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException {
		throw new NoCachingEnabledException();
	}

	public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties) throws CacheException {
		throw new NoCachingEnabledException();
	}

	public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties) throws CacheException {
		throw new NoCachingEnabledException();
	}
}

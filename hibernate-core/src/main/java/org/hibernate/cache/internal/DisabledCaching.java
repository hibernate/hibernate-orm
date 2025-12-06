/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;

import java.util.Collections;
import java.util.Set;

import jakarta.persistence.PersistenceException;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsCache;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.NavigableRole;

/**
 * A {@link CacheImplementor} service used when the second-level cache is disabled.
 *
 * @author Steve Ebersole
 */
public class DisabledCaching implements CacheImplementor {
	private final SessionFactoryImplementor sessionFactory;
	private final RegionFactory regionFactory;

	public DisabledCaching(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		this.regionFactory = sessionFactory.getServiceRegistry().getService( RegionFactory.class );
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public RegionFactory getRegionFactory() {
		return regionFactory;
	}

	@Override
	public void prime(Set<DomainDataRegionConfig> cacheRegionConfigs) {
		// nothing to do
	}

	@Override
	public boolean containsEntity(Class<?> entityClass, Object identifier) {
		return false;
	}

	@Override
	public boolean containsEntity(String entityName, Object identifier) {
		return false;
	}

	@Override
	public void evictEntityData(Class<?> entityClass, Object identifier) {
		// nothing to do
	}

	@Override
	public void evictEntityData(String entityName, Object identifier) {
		// nothing to do
	}

	@Override
	public void evictEntityData(Class<?> entityClass) {
		// nothing to do
	}

	@Override
	public void evictEntityData(String entityName) {
		// nothing to do
	}

	@Override
	public void evictEntityData() {
		// nothing to do
	}

	@Override
	public void evictNaturalIdData(Class<?> entityClass) {
		// nothing to do
	}

	@Override
	public void evictNaturalIdData(String entityName) {
		// nothing to do
	}

	@Override
	public void evictNaturalIdData() {
		// nothing to do
	}

	@Override
	public boolean containsCollection(String role, Object ownerIdentifier) {
		return false;
	}

	@Override
	public void evictCollectionData(String role, Object ownerIdentifier) {
		// nothing to do
	}

	@Override
	public void evictCollectionData(String role) {
		// nothing to do
	}

	@Override
	public void evictCollectionData() {
		// nothing to do
	}

	@Override
	public boolean containsQuery(String regionName) {
		return false;
	}

	@Override
	public void evictDefaultQueryRegion() {
		// nothing to do
	}

	@Override
	public void evictQueryRegion(String regionName) {
		// nothing to do
	}

	@Override
	public void evictQueryRegions() {
		// nothing to do
	}

	@Override
	public void evictRegion(String regionName) {
		// nothing to do
	}


	@Override
	public void evictAll() {
		// nothing to do
	}

	@Override
	public void evictAllRegions() {
		// nothing to do
	}

	@Override
	public Region getRegion(String fullRegionName) {
		return null;
	}

	@Override
	public TimestampsCache getTimestampsCache() {
		return null;
	}

	@Override
	public QueryResultsCache getDefaultQueryResultsCache() {
		return null;
	}

	@Override
	public QueryResultsCache getQueryResultsCache(String regionName) {
		return null;
	}

	@Override
	public QueryResultsCache getQueryResultsCacheStrictly(String regionName) {
		return null;
	}

	@Override
	public void close() {
	}

	@Override
	public Set<String> getCacheRegionNames() {
		return Collections.emptySet();
	}

	@Override @Deprecated
	public EntityDataAccess getEntityRegionAccess(NavigableRole rootEntityName) {
		return null;
	}

	@Override @Deprecated
	public NaturalIdDataAccess getNaturalIdCacheRegionAccessStrategy(NavigableRole rootEntityName) {
		return null;
	}

	@Override @Deprecated
	public CollectionDataAccess getCollectionRegionAccess(NavigableRole collectionRole) {
		return null;
	}

	@Override
	public boolean contains(Class cls, Object primaryKey) {
		return false;
	}

	@Override
	public void evict(Class cls, Object primaryKey) {
		// nothing to do
	}

	@Override
	public void evict(Class cls) {
		// nothing to do
	}

	@Override
	public <T> T unwrap(Class<T> type) {
		if ( type.isAssignableFrom( DisabledCaching.class ) ) {
			return type.cast( this );
		}
		else {
			throw new PersistenceException( "Hibernate cannot unwrap Cache as '" + type.getName() + "'" );
		}
	}
}

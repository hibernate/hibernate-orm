/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.internal;

import java.io.Serializable;
import java.util.Set;

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
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * CacheImplementor implementation for disabled caching
 *
 * @author Steve Ebersole
 */
public class DisabledCaching implements CacheImplementor {
	private final SessionFactoryImplementor sessionFactory;

	public DisabledCaching(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void prime(Set<DomainDataRegionConfig> cacheRegionConfigs) {
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public RegionFactory getRegionFactory() {
		return null;
	}

	@Override
	public boolean containsEntity(Class entityClass, Serializable identifier) {
		return false;
	}

	@Override
	public boolean containsEntity(String entityName, Serializable identifier) {
		return false;
	}

	@Override
	public void evictEntity(Class entityClass, Serializable identifier) {
		// nothing to do
	}

	@Override
	public void evictEntity(String entityName, Serializable identifier) {
		// nothing to do
	}

	@Override
	public void evictEntityRegion(Class entityClass) {
		// nothing to do
	}

	@Override
	public void evictEntityRegion(String entityName) {
		// nothing to do
	}

	@Override
	public void evictEntityRegions() {
		// nothing to do
	}

	@Override
	public void evictNaturalIdRegion(Class entityClass) {
		// nothing to do
	}

	@Override
	public void evictNaturalIdRegion(String entityName) {
		// nothing to do
	}

	@Override
	public void evictNaturalIdRegions() {
		// nothing to do
	}

	@Override
	public boolean containsCollection(String role, Serializable ownerIdentifier) {
		return false;
	}

	@Override
	public void evictCollection(String role, Serializable ownerIdentifier) {
		// nothing to do
	}

	@Override
	public void evictCollectionRegion(String role) {
		// nothing to do
	}

	@Override
	public void evictCollectionRegions() {
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
	public void evictAllRegions() {
		// nothing to do
	}



	@Override
	public Region getRegion(String fullRegionName) {
		return null;
	}

//	@Override
//	public EntityDataAccess getEntityRegionAccess(EntityDescriptor rootEntityDescriptor) {
//		return null;
//	}
//
//	@Override
//	public NaturalIdDataAccess getNaturalIdRegionAccess(EntityDescriptor rootEntityDescriptor) {
//		return null;
//	}
//
//	@Override
//	public CollectionDataAccess getCollectionRegionAccess(PersistentCollectionDescriptor collectionDescriptor) {
//		return null;
//	}

	@Override
	public TimestampsCache getTimestampsRegionAccess() {
		return null;
	}

	@Override
	public QueryResultsCache getDefaultQueryResultsRegionAccess() {
		return null;
	}

	@Override
	public QueryResultsCache getQueryResultsCache(String regionName) {
		return null;
	}

	@Override
	public QueryResultsCache getOrMakeQueryResultsRegionAccess(String regionName) {
		return null;
	}

	@Override
	public void close() {
	}

	@Override
	public String[] getSecondLevelCacheRegionNames() {
		return new String[0];
	}

	@Override
	public EntityDataAccess getEntityRegionAccess(EntityHierarchy hierarchy) {
		return null;
	}

	@Override
	public NaturalIdDataAccess getNaturalIdRegionAccess(EntityHierarchy hierarchy) {
		return null;
	}

	@Override
	public CollectionDataAccess getCollectionRegionAccess(PersistentCollectionDescriptor collectionDescriptor) {
		return null;
	}

	@Override
	public boolean contains(Class cls, Object primaryKey) {
		return false;
	}

	@Override
	public void evict(Class cls, Object primaryKey) {

	}

	@Override
	public void evict(Class cls) {

	}

	@Override
	public void evictAll() {

	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		return null;
	}

}

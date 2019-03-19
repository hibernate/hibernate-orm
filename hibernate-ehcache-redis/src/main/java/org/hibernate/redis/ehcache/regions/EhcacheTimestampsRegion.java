package org.hibernate.redis.ehcache.regions;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.Cacheable;
import javax.persistence.metamodel.EntityType;

import org.hibernate.redis.ehcache.strategy.EhcacheAccessStrategyFactory;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.EhCacheMessageLogger;
import org.hibernate.cache.ehcache.internal.nonstop.HibernateNonstopCacheExceptionHandler;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.jboss.logging.Logger;
import org.redisson.api.BatchOptions;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RedissonClient;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

public class EhcacheTimestampsRegion extends EhcacheGeneralDataRegion implements TimestampsRegion {
	private static final EhCacheMessageLogger LOG = Logger.getMessageLogger(EhCacheMessageLogger.class,
			EhcacheTimestampsRegion.class.getName());

	private static Boolean metadataInitialized = false;
	private static final Set<Serializable> cacheableSpacesSet = java.util.Collections
			.newSetFromMap(new ConcurrentHashMap<>());

	/**
	 * @param accessStrategyFactory
	 * @param underlyingCache
	 * @param properties
	 * @param redisson
	 * @param batchOptions
	 * @param localCachedMapOptions
	 */
	public EhcacheTimestampsRegion(EhcacheAccessStrategyFactory accessStrategyFactory, Ehcache underlyingCache,
			Properties properties, RedissonClient redisson, BatchOptions batchOptions,
			LocalCachedMapOptions<Object, Object> localCachedMapOptions) {
		super(accessStrategyFactory, underlyingCache, properties, redisson, batchOptions, localCachedMapOptions);
	}

	@Override
	public Object get(SharedSessionContractImplementor session, Object key) throws CacheException {
		try {
			LOG.debugf("key: %s", key);
			if (key == null) {
				return null;
			} else {
				final Object element = getDistributedNeutrinoCache().get(key);
				if (element == null) {
					LOG.debugf("Timestamp for key %s is null", key);
					return null;
				}
				return element;
			}
		} catch (net.sf.ehcache.CacheException e) {
			if (e instanceof NonStopCacheException) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException((NonStopCacheException) e);
				return null;
			} else {
				throw new CacheException(e);
			}
		}
	}

	@Override
	public void put(SharedSessionContractImplementor session, Object key, Object value) throws CacheException {
		LOG.debugf("key: %s value: %s", key, value);
		if (!metadataInitialized) {
			initializeMetadata(session);
		}
		try {
			if (cacheableSpacesSet.contains(key)) {
				getDistributedNeutrinoCache().put(key, value);
			}
		} catch (IllegalArgumentException e) {
			throw new CacheException(e);
		} catch (IllegalStateException e) {
			throw new CacheException(e);
		} catch (net.sf.ehcache.CacheException e) {
			if (e instanceof NonStopCacheException) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException((NonStopCacheException) e);
			} else {
				throw new CacheException(e);
			}
		}
	}

	@Override
	public void evict(Object key) throws CacheException {
		try {
			getDistributedNeutrinoCache().remove(key);
		} catch (ClassCastException e) {
			throw new CacheException(e);
		} catch (IllegalStateException e) {
			throw new CacheException(e);
		} catch (net.sf.ehcache.CacheException e) {
			if (e instanceof NonStopCacheException) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException((NonStopCacheException) e);
			} else {
				throw new CacheException(e);
			}
		}
	}

	@Override
	public void evictAll() throws CacheException {
		try {
			getDistributedNeutrinoCache().clear();
		} catch (IllegalStateException e) {
			throw new CacheException(e);
		} catch (net.sf.ehcache.CacheException e) {
			if (e instanceof NonStopCacheException) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException((NonStopCacheException) e);
			} else {
				throw new CacheException(e);
			}
		}
	}

	private synchronized void initializeMetadata(SharedSessionContractImplementor session) {
		SessionFactoryImplementor factory = session.getFactory();
		if (!metadataInitialized && factory != null && factory.getMetamodel() != null
				&& factory.getMetamodel().getEntities() != null
				&& factory.getMetamodel().collectionPersisters() != null) {

			Set<EntityType<?>> entities = factory.getMetamodel().getEntities();
			for (EntityType<?> en : entities) {
				Class<?> clas = en.getJavaType();
				Cacheable cacheable = clas.getDeclaredAnnotation(Cacheable.class);
				if (cacheable != null) {
					cacheableSpacesSet.add(factory.getMetamodel().locateEntityPersister(clas).getPropertySpaces()[0]);
				}
			}

			Map<String, CollectionPersister> collectionPersister = factory.getMetamodel().collectionPersisters();
			for (Map.Entry<String, CollectionPersister> entry : collectionPersister.entrySet()) {
				if (entry.getValue().getCacheAccessStrategy() != null) {
					for (Serializable space : entry.getValue().getCollectionSpaces()) {
						cacheableSpacesSet.add(space);
					}
				}
			}
		}
		metadataInitialized = true;
	}
}

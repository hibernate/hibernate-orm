/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.session;

import org.hibernate.*;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.NonFlushedChanges;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.shards.CrossShardAssociationException;
import org.hibernate.shards.Shard;
import org.hibernate.shards.ShardId;
import org.hibernate.shards.ShardImpl;
import org.hibernate.shards.ShardOperation;
import org.hibernate.shards.ShardedTransaction;
import org.hibernate.shards.criteria.CriteriaFactoryImpl;
import org.hibernate.shards.criteria.CriteriaId;
import org.hibernate.shards.criteria.ShardedCriteriaImpl;
import org.hibernate.shards.engine.ShardedSessionFactoryImplementor;
import org.hibernate.shards.engine.ShardedSessionImplementor;
import org.hibernate.shards.id.ShardEncodingIdentifierGenerator;
import org.hibernate.shards.internal.ShardsMessageLogger;
import org.hibernate.shards.query.AdHocQueryFactoryImpl;
import org.hibernate.shards.query.ExitOperationsQueryCollector;
import org.hibernate.shards.query.NamedQueryFactoryImpl;
import org.hibernate.shards.query.QueryId;
import org.hibernate.shards.query.ShardedQueryImpl;
import org.hibernate.shards.query.ShardedSQLQueryImpl;
import org.hibernate.shards.stat.ShardedSessionStatistics;
import org.hibernate.shards.strategy.ShardStrategy;
import org.hibernate.shards.strategy.exit.FirstNonNullResultExitStrategy;
import org.hibernate.shards.strategy.selection.ShardResolutionStrategyData;
import org.hibernate.shards.strategy.selection.ShardResolutionStrategyDataImpl;
import org.hibernate.shards.engine.internal.ShardedTransactionImpl;
import org.hibernate.shards.util.Iterables;
import org.hibernate.shards.util.Lists;
import org.hibernate.shards.util.Maps;
import org.hibernate.shards.util.Pair;
import org.hibernate.shards.util.Preconditions;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.type.Type;
import org.jboss.logging.Logger;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Concrete implementation of a ShardedSession, and also the central component of
 * Hibernate Shards' internal implementation. This class exposes two interfaces;
 * ShardedSession itself, to the application, and ShardedSessionImplementor, to
 * other components of Hibernate Shards. This class is not threadsafe.
 *
 * @author maxr@google.com (Max Ross)
 *         Tomislav Nad
 */
public class ShardedSessionImpl implements ShardedSession, ShardedSessionImplementor, ShardIdResolver {

    public static final ShardsMessageLogger LOG = Logger.getMessageLogger(ShardsMessageLogger.class, ShardedSessionImpl.class.getName());

    private static ThreadLocal<ShardId> currentSubgraphShardId = new ThreadLocal<ShardId>();

    private final ShardedSessionFactoryImplementor shardedSessionFactory;
    private final List<Shard> shards;
    private final Map<ShardId, Shard> shardIdsToShards;
    private final ShardStrategy shardStrategy;
    private final Set<Class<?>> classesWithoutTopLevelSaveSupport;
    private final boolean checkAllAssociatedObjectsForDifferentShards;

    private ShardedTransaction transaction;
    private boolean closed = false;
    private boolean lockedShard = false;
    private ShardId lockedShardId;

    // access to sharded session is single-threaded so we can use a non-atomic
    // counter for criteria ids or query ids
    private int nextCriteriaId = 0;
    private int nextQueryId = 0;

    /**
     * Constructor used for openSession(...) processing.
     *
     * @param shardedSessionFactory The factory from which this session was obtained
     * @param shardStrategy         The shard strategy for this session
     * @param classesWithoutTopLevelSaveSupport
     *                              The set of classes on which top-level save can not be performed
     * @param checkAllAssociatedObjectsForDifferentShards
     *                              Should we check for cross-shard relationships
     */
    ShardedSessionImpl(final ShardedSessionFactoryImplementor shardedSessionFactory,
                       final ShardStrategy shardStrategy,
                       final Set<Class<?>> classesWithoutTopLevelSaveSupport,
                       final boolean checkAllAssociatedObjectsForDifferentShards) {
        this(null, shardedSessionFactory, shardStrategy, classesWithoutTopLevelSaveSupport,
                checkAllAssociatedObjectsForDifferentShards);
    }

    /**
     * Constructor used for openSession(...) processing.
     *
     * @param interceptor           The interceptor to be applied to this session
     * @param shardedSessionFactory The factory from which this session was obtained
     * @param shardStrategy         The shard strategy for this session
     * @param classesWithoutTopLevelSaveSupport
     *                              The set of classes on which top-level save can not be performed
     * @param checkAllAssociatedObjectsForDifferentShards
     *                              Should we check for cross-shard relationships
     */
    ShardedSessionImpl(final /*@Nullable*/ Interceptor interceptor,
                       final ShardedSessionFactoryImplementor shardedSessionFactory,
                       final ShardStrategy shardStrategy,
                       final Set<Class<?>> classesWithoutTopLevelSaveSupport,
                       final boolean checkAllAssociatedObjectsForDifferentShards) {

        this.shardedSessionFactory = shardedSessionFactory;
        this.shards = buildShardListFromSessionFactoryShardIdMap(shardedSessionFactory.getSessionFactoryShardIdMap(),
                checkAllAssociatedObjectsForDifferentShards, this, interceptor);
        this.shardIdsToShards = buildShardIdsToShardsMap();
        this.shardStrategy = shardStrategy;
        this.classesWithoutTopLevelSaveSupport = classesWithoutTopLevelSaveSupport;
        this.checkAllAssociatedObjectsForDifferentShards = checkAllAssociatedObjectsForDifferentShards;
    }

    @Override
    public List<Shard> getShards() {
        return Collections.unmodifiableList(shards);
    }

    public Object get(final Class clazz, final Serializable id) throws HibernateException {
        final ShardOperation<Object> shardOp = new ShardOperation<Object>() {
            public Object execute(Shard shard) {
                return shard.establishSession().get(clazz, id);
            }

            public String getOperationName() {
                return "get(Class class, Serializable id)";
            }
        };

        return applyGetOperation(shardOp, new ShardResolutionStrategyDataImpl(clazz, id));
    }

    @Override
    @Deprecated
    public Object get(final Class clazz, final Serializable id, final LockMode lockMode) throws HibernateException {
        return get(clazz, id, new LockOptions(lockMode));
    }

    @Override
    public Object get(final Class clazz, final Serializable id, final LockOptions lockOptions) {
        final ShardOperation<Object> shardOp = new ShardOperation<Object>() {
            public Object execute(final Shard shard) {
                return shard.establishSession().get(clazz, id, lockOptions);
            }

            public String getOperationName() {
                return "get(Class class, Serializable id, LockMode lockMode)";
            }
        };
        // we're not letting people customize shard selection by lockMode
        return applyGetOperation(shardOp, new ShardResolutionStrategyDataImpl(clazz, id));
    }

    public Object get(final String entityName, final Serializable id) throws HibernateException {
        final ShardOperation<Object> shardOp = new ShardOperation<Object>() {
            public Object execute(final Shard shard) {
                return shard.establishSession().get(entityName, id);
            }

            public String getOperationName() {
                return "get(String entityName, Serializable id)";
            }
        };
        return applyGetOperation(shardOp, new ShardResolutionStrategyDataImpl(entityName, id));
    }

    @Override
    @Deprecated
    public Object get(final String entityName, final Serializable id, final LockMode lockMode) throws HibernateException {
        return get(entityName, id, new LockOptions(lockMode));
    }

    @Override
    public Object get(final String entityName, final Serializable id, final LockOptions lockOptions) {
        final ShardOperation<Object> shardOp = new ShardOperation<Object>() {
            public Object execute(final Shard shard) {
                return shard.establishSession().get(entityName, id, lockOptions);
            }

            public String getOperationName() {
                return "get(String entityName, Serializable id, LockOptions lockOptions)";
            }
        };
        // we're not letting people customize shard selection by lockMode
        return applyGetOperation(shardOp, new ShardResolutionStrategyDataImpl(entityName, id));
    }

    @Override
    public SharedSessionBuilder sessionWithOptions() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void flush() throws HibernateException {
        for (final Shard shard : shards) {
            // unopened sessions won't have anything to flush
            if (shard.getSession() != null) {
                shard.getSession().flush();
            }
        }
    }

    @Override
    public void setFlushMode(final FlushMode flushMode) {
        final SetFlushModeOpenSessionEvent event = new SetFlushModeOpenSessionEvent(flushMode);
        for (final Shard shard : shards) {
            if (shard.getSession() != null) {
                shard.getSession().setFlushMode(flushMode);
            } else {
                shard.addOpenSessionEvent(event);
            }
        }
    }

    @Override
    public Connection connection() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FlushMode getFlushMode() {
        // all shards must have the same flush mode
        Session someSession = getSomeSession();
        if (someSession == null) {
            someSession = shards.get(0).establishSession();
        }
        return someSession.getFlushMode();
    }

    @Override
    public void setCacheMode(final CacheMode cacheMode) {
        final SetCacheModeOpenSessionEvent event = new SetCacheModeOpenSessionEvent(cacheMode);
        for (final Shard shard : shards) {
            if (shard.getSession() != null) {
                shard.getSession().setCacheMode(cacheMode);
            } else {
                shard.addOpenSessionEvent(event);
            }
        }
    }

    @Override
    public CacheMode getCacheMode() {
        // all shards must have the same cache mode
        Session someSession = getSomeSession();
        if (someSession == null) {
            someSession = shards.get(0).establishSession();
        }
        return someSession.getCacheMode();
    }

    @Override
    public ShardedSessionFactoryImplementor getSessionFactory() {
        return shardedSessionFactory;
    }

    @Override
    public Connection close() throws HibernateException {
        List<Throwable> thrown = null;
        for (final Shard shard : shards) {
            if (shard.getSession() != null) {
                try {
                    shard.getSession().close();
                } catch (Throwable t) {
                    if (thrown == null) {
                        thrown = Lists.newArrayList();
                    }
                    thrown.add(t);
                    // we're going to try and close everything that was
                    // opened
                }
            }
        }

        shards.clear();
        shardIdsToShards.clear();
        classesWithoutTopLevelSaveSupport.clear();

        if (thrown != null && !thrown.isEmpty()) {
            // we'll just throw the first one
            final Throwable first = thrown.get(0);
            if (HibernateException.class.isAssignableFrom(first.getClass())) {
                throw (HibernateException) first;
            }
            throw new HibernateException(first);
        }
        closed = true;
        // TODO(maxr) what should I return here?
        return null;
    }

    @Override
    public void cancelQuery() throws HibernateException {
        // cancel across all shards
        for (final Shard shard : shards) {
            if (shard.getSession() != null) {
                shard.getSession().cancelQuery();
            }
        }
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public boolean isConnected() {
        // one connected shard means the session as a whole is connected
        for (final Shard shard : shards) {
            if (shard.getSession() != null && shard.getSession().isConnected()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDirty() throws HibernateException {
        // one dirty shard is all it takes
        for (final Shard shard : shards) {
            if (shard.getSession() != null && shard.getSession().isDirty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDefaultReadOnly() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setDefaultReadOnly(boolean readOnly) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Serializable getIdentifier(Object object) throws HibernateException {
        for (final Shard shard : shards) {
            if (shard.getSession() != null) {
                try {
                    return shard.getSession().getIdentifier(object);
                } catch (TransientObjectException e) {
                    // Object is transient or is not associated with this session.
                }
            }
        }
        throw new TransientObjectException("Instance is transient or associated with a defferent Session");
    }

    @Override
    public boolean contains(final Object object) {
        for (final Shard shard : shards) {
            if (shard.getSession() != null && shard.getSession().contains(object)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void evict(final Object object) throws HibernateException {
        for (final Shard shard : shards) {
            if (shard.getSession() != null) {
                shard.getSession().evict(object);
            }
        }
    }

    @Override
    @Deprecated
    public Object load(final Class clazz, final Serializable id, final LockMode lockMode) throws HibernateException {
        return load(clazz, id, new LockOptions(lockMode));
    }

    @Override
    public Object load(final Class theClass, final Serializable id, final LockOptions lockOptions) {
        final List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(new
                ShardResolutionStrategyDataImpl(theClass, id));

        if (shardIds.size() == 1) {
            return shardIdsToShards.get(shardIds.get(0)).establishSession().load(theClass, id, lockOptions);
        } else {
            final Object result = get(theClass, id, lockOptions);
            if (result == null) {
                shardedSessionFactory.getEntityNotFoundDelegate().handleEntityNotFound(theClass.getName(), id);
            }
            return result;
        }
    }

    @Override
    @Deprecated
    public Object load(final String entityName, final Serializable id, final LockMode lockMode)
            throws HibernateException {
        return load(entityName, id, new LockOptions(lockMode));
    }

    @Override
    public Object load(final String entityName, final Serializable id, final LockOptions lockOptions) {
        final List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(new
                ShardResolutionStrategyDataImpl(entityName, id));

        if (shardIds.size() == 1) {
            return shardIdsToShards.get(shardIds.get(0)).establishSession().load(entityName, id, lockOptions);
        } else {
            final Object result = get(entityName, id, lockOptions);
            if (result == null) {
                shardedSessionFactory.getEntityNotFoundDelegate().handleEntityNotFound(entityName, id);
            }
            return result;
        }
    }

    @Override
    public Object load(final Class clazz, final Serializable id) throws HibernateException {
        final List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(new
                ShardResolutionStrategyDataImpl(clazz, id));

        if (shardIds.size() == 1) {
            return shardIdsToShards.get(shardIds.get(0)).establishSession().load(clazz, id);
        } else {
            final Object result = get(clazz, id);
            if (result == null) {
                shardedSessionFactory.getEntityNotFoundDelegate().handleEntityNotFound(clazz.getName(), id);
            }
            return result;
        }
    }

    @Override
    public Object load(final String entityName, final Serializable id) throws HibernateException {
        final List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(new
                ShardResolutionStrategyDataImpl(entityName, id));

        if (shardIds.size() == 1) {
            return shardIdsToShards.get(shardIds.get(0)).establishSession().load(entityName, id);
        } else {
            final Object result = get(entityName, id);
            if (result == null) {
                shardedSessionFactory.getEntityNotFoundDelegate().handleEntityNotFound(entityName, id);
            }
            return result;
        }
    }

    @Override
    public void load(final Object object, final Serializable id) throws HibernateException {
        final List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(new
                ShardResolutionStrategyDataImpl(object.getClass(), id));

        if (shardIds.size() == 1) {
            shardIdsToShards.get(shardIds.get(0)).establishSession().load(object, id);
        } else {
            final Object result = get(object.getClass(), id);
            if (result == null) {
                shardedSessionFactory.getEntityNotFoundDelegate().handleEntityNotFound(object.getClass().getName(), id);
            } else {
                final Shard objectShard = getShardForObject(result, shardIdListToShardList(shardIds));
                evict(result);
                objectShard.establishSession().load(object, id);
            }
        }
    }

    @Override
    public void replicate(final Object object, final ReplicationMode replicationMode) throws HibernateException {
        replicate(null, object, replicationMode);
    }

    @Override
    public void replicate(final String entityName, final Object object, ReplicationMode replicationMode)
            throws HibernateException {
        final Serializable id = extractId(object);
        final List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(new
                ShardResolutionStrategyDataImpl(object.getClass(), id));

        if (shardIds.size() == 1) {
            setCurrentSubgraphShardId(shardIds.get(0));
            shardIdsToShards.get(shardIds.get(0)).establishSession().replicate(entityName, object, replicationMode);
        } else {
            Object result = null;
            if (id != null) result = get(object.getClass(), id);
            if (result == null) {  // non-persisted object
                final ShardId shardId = selectShardIdForNewObject(object);
                setCurrentSubgraphShardId(shardId);
                shardIdsToShards.get(shardId).establishSession().replicate(entityName, object, replicationMode);
            } else {
                Shard objectShard = getShardForObject(result, shardIdListToShardList(shardIds));
                evict(result);
                objectShard.establishSession().replicate(entityName, object, replicationMode);
            }
        }
    }

    @Override
    public Serializable save(final String entityName, final Object object) throws HibernateException {
        // TODO(tomislav): what if we have detached instance?
        ShardId shardId = getShardIdForObject(object);
        if (shardId == null) {
            shardId = selectShardIdForNewObject(object);
        }
        Preconditions.checkNotNull(shardId);
        setCurrentSubgraphShardId(shardId);
        LOG.debugf("Saving object of type %s to shard %s", object.getClass(), shardId);
        return shardIdsToShards.get(shardId).establishSession().save(entityName, object);
    }

    @Override
    public Serializable save(final Object object) throws HibernateException {
        return save(null, object);
    }

    @Override
    public void saveOrUpdate(final Object object) throws HibernateException {
        applySaveOrUpdateOperation(new SimpleSaveOrUpdateOperation(), object);
    }

    @Override
    public void saveOrUpdate(final String entityName, final Object object) throws HibernateException {
        applySaveOrUpdateOperation(new EntityNameSaveOrUpdateOperation(entityName), object);
    }

    @Override
    public void update(final Object object) throws HibernateException {
        applyUpdateOperation(new SimpleUpdateOperation(), object);
    }

    @Override
    public void update(final String entityName, final Object object) throws HibernateException {
        applyUpdateOperation(new EntityNameUpdateOperation(entityName), object);
    }

    @Override
    public Object merge(final Object object) throws HibernateException {
        return merge(null, object);
    }

    @Override
    public Object merge(final String entityName, final Object object) throws HibernateException {
        final Serializable id = extractId(object);
        final List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(new
                ShardResolutionStrategyDataImpl(object.getClass(), id));

        if (shardIds.size() == 1) {
            setCurrentSubgraphShardId(shardIds.get(0));
            return shardIdsToShards.get(shardIds.get(0)).establishSession().merge(entityName, object);
        } else {
            Object result = null;
            if (id != null) result = get(object.getClass(), id);
            if (result == null) {  // non-persisted object
                final ShardId shardId = selectShardIdForNewObject(object);
                setCurrentSubgraphShardId(shardId);
                return shardIdsToShards.get(shardId).establishSession().merge(entityName, object);
            } else {
                final Shard objectShard = getShardForObject(result, shardIdListToShardList(shardIds));
                return objectShard.establishSession().merge(entityName, object);
            }
        }
    }

    @Override
    public void persist(final Object object) throws HibernateException {
        persist(null, object);
    }

    @Override
    public void persist(final String entityName, final Object object) throws HibernateException {
        // TODO(tomislav): what if we have detached object?
        ShardId shardId = getShardIdForObject(object);
        if (shardId == null) {
            shardId = selectShardIdForNewObject(object);
        }
        Preconditions.checkNotNull(shardId);
        setCurrentSubgraphShardId(shardId);
        LOG.debugf("Persisting object of type %s to shard %s", object.getClass(), shardId);
        shardIdsToShards.get(shardId).establishSession().persist(entityName, object);
    }

    @Override
    public void delete(Object object) throws HibernateException {
        applyDeleteOperation(new SimpleDeleteOperation(), object);
    }

    public void delete(final String entityName, final Object object) throws HibernateException {
        applyDeleteOperation(new EntityNameDeleteOperation(entityName), object);
    }

    @Override
    @Deprecated
    public void lock(final Object object, final LockMode lockMode) throws HibernateException {
        final ShardOperation<Void> op = new ShardOperation<Void>() {
            public Void execute(Shard s) {
                s.establishSession().lock(object, lockMode);
                return null;
            }

            public String getOperationName() {
                return "lock(Object object, LockMode lockMode)";
            }
        };
        invokeOnShardWithObject(op, object);
    }

    @Override
    @Deprecated
    public void lock(final String entityName, final Object object, final LockMode lockMode) throws HibernateException {
        //lock(entityName, object, new LockOptions(lockMode));
        final ShardOperation<Void> op = new ShardOperation<Void>() {
            public Void execute(Shard s) {
                s.establishSession().lock(entityName, object, lockMode);
                return null;
            }

            public String getOperationName() {
                return "lock(String entityName, Object object, LockMode lockMode)";
            }
        };
        invokeOnShardWithObject(op, object);
    }

    @Override
    public LockRequest buildLockRequest(final LockOptions lockOptions) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void refresh(final Object object) throws HibernateException {
        final RefreshOperation op = new DefaultRefreshOperation();
        applyRefreshOperation(op, object);
    }

    @Override
    public void refresh(final String entityName, final Object object) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @Deprecated
    public void refresh(final Object object, final LockMode lockMode) throws HibernateException {
        refresh(object, new LockOptions(lockMode));
    }

    @Override
    public void refresh(final Object object, final LockOptions lockOptions) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void refresh(final String entityName, final Object object, final LockOptions lockOptions) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public LockMode getCurrentLockMode(final Object object) throws HibernateException {
        final ShardOperation<LockMode> invoker = new ShardOperation<LockMode>() {
            public LockMode execute(Shard s) {
                return s.establishSession().getCurrentLockMode(object);
            }

            public String getOperationName() {
                return "getCurrentLockmode(Object object)";
            }
        };
        return invokeOnShardWithObject(invoker, object);
    }

    @Override
    public String getTenantIdentifier() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public JdbcConnectionAccess getJdbcConnectionAccess() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public EntityKey generateEntityKey(Serializable id, EntityPersister persister) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public CacheKey generateCacheKey(Serializable id, Type type, String entityOrRoleName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Interceptor getInterceptor() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setAutoClear(boolean enabled) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void disableTransactionAutoJoin() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isTransactionInProgress() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void initializeCollection(PersistentCollection collection, boolean writing) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object internalLoad(String entityName, Serializable id, boolean eager, boolean nullable) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object immediateLoad(String entityName, Serializable id) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getTimestamp() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SessionFactoryImplementor getFactory() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List list(String query, QueryParameters queryParameters) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ScrollableResults scroll(String query, QueryParameters queryParameters) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ScrollableResults scroll(CriteriaImpl criteria, ScrollMode scrollMode) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List list(CriteriaImpl criteria) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List listFilter(Object collection, String filter, QueryParameters queryParameters) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public EntityPersister getEntityPersister(String entityName, Object object) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Serializable getContextEntityIdentifier(Object object) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String bestGuessEntityName(Object object) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String guessEntityName(Object entity) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object instantiate(String entityName, Serializable id) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ScrollableResults scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List list(NativeSQLQuerySpecification spec, QueryParameters queryParameters) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ScrollableResults scroll(NativeSQLQuerySpecification spec, QueryParameters queryParameters) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @Deprecated
    public Object getFilterParameterValue(String filterParameterName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @Deprecated
    public Type getFilterParameterType(String filterParameterName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @Deprecated
    public Map getEnabledFilters() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getDontFlushFromFind() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PersistenceContext getPersistenceContext() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int executeNativeUpdate(NativeSQLQuerySpecification specification, QueryParameters queryParameters) throws HibernateException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NonFlushedChanges getNonFlushedChanges() throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void applyNonFlushedChanges(NonFlushedChanges nonFlushedChanges) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Transaction beginTransaction() throws HibernateException {
        errorIfClosed();
        Transaction result = getTransaction();
        result.begin();
        return result;
    }

    @Override
    public Transaction getTransaction() {
        errorIfClosed();
        if (transaction == null) {
            transaction = new ShardedTransactionImpl(this, null);
        }
        return transaction;
    }

    @Override
    public Criteria createCriteria(final Class persistentClass) {
        return new ShardedCriteriaImpl(new CriteriaId(nextCriteriaId++),
                shards,
                new CriteriaFactoryImpl(persistentClass),
                shardStrategy.getShardAccessStrategy());
    }

    @Override
    public Criteria createCriteria(final Class persistentClass, final String alias) {
        return new ShardedCriteriaImpl(new CriteriaId(nextCriteriaId++),
                shards,
                new CriteriaFactoryImpl(persistentClass, alias),
                shardStrategy.getShardAccessStrategy());
    }

    @Override
    public Criteria createCriteria(final String entityName) {
        return new ShardedCriteriaImpl(
                new CriteriaId(nextCriteriaId++),
                shards,
                new CriteriaFactoryImpl(entityName),
                shardStrategy.getShardAccessStrategy());
    }

    @Override
    public Criteria createCriteria(final String entityName, String alias) {
        return new ShardedCriteriaImpl(
                new CriteriaId(nextCriteriaId++),
                shards,
                new CriteriaFactoryImpl(entityName, alias),
                shardStrategy.getShardAccessStrategy());
    }

    @Override
    public Query createQuery(final String queryString) throws HibernateException {
        return new ShardedQueryImpl(new QueryId(nextQueryId++),
                shards,
                new AdHocQueryFactoryImpl(queryString),
                shardStrategy.getShardAccessStrategy());
    }

    /**
     * Unsupported.  This is a scope decision, not a technical decision.
     */
    @Override
    public SQLQuery createSQLQuery(final String queryString) throws HibernateException {
        return new ShardedSQLQueryImpl(new QueryId(nextQueryId++),
                shards,
                new AdHocQueryFactoryImpl(queryString),
                shardStrategy.getShardAccessStrategy());
    }

    /**
     * The {@link org.hibernate.internal.SessionImpl#createFilter(Object, String)} implementation
     * requires that the collection that is passed in is a persistent collection.
     * Since we don't support cross-shard relationships, if we receive a persistent
     * collection that collection is guaranteed to be associated with a single
     * shard.  If we can figure out which shard the collection is associated with
     * we can just delegate this operation to that shard.
     */
    @Override
    public Query createFilter(final Object collection, final String queryString) throws HibernateException {
        final Shard shard = getShardForCollection(collection, shards);
        Session session;
        if (shard == null) {
            // collection not associated with any of our shards, so just delegate to
            // a random shard.  We'll end up failing, but we'll fail with the
            // error that users typically get.
            session = getSomeSession();
            if (session == null) {
                session = shards.get(0).establishSession();
            }
        } else {
            session = shard.establishSession();
        }
        return session.createFilter(collection, queryString);
    }

    @Override
    public Query getNamedQuery(final String queryName) throws HibernateException {
        return new ShardedQueryImpl(new QueryId(nextQueryId++),
                shards,
                new NamedQueryFactoryImpl(queryName),
                shardStrategy.getShardAccessStrategy());
    }

    @Override
    public Query getNamedSQLQuery(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isEventSource() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void afterScrollOperation() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @Deprecated
    public String getFetchProfile() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @Deprecated
    public void setFetchProfile(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TransactionCoordinator getTransactionCoordinator() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isClosed() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public LoadQueryInfluencers getLoadQueryInfluencers() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void clear() {
        for (final Shard shard : shards) {
            if (shard.getSession() != null) {
                shard.getSession().clear();
            }
        }
    }

    @Override
    public String getEntityName(final Object object) throws HibernateException {
        final ShardOperation<String> invoker = new ShardOperation<String>() {
            public String execute(Shard s) {
                return s.establishSession().getEntityName(object);
            }

            public String getOperationName() {
                return "getEntityName(Object object)";
            }
        };
        return invokeOnShardWithObject(invoker, object);
    }

    @Override
    public IdentifierLoadAccess byId(final String entityName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public IdentifierLoadAccess byId(final Class entityClass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NaturalIdLoadAccess byNaturalId(final String entityName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NaturalIdLoadAccess byNaturalId(final Class entityClass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SimpleNaturalIdLoadAccess bySimpleNaturalId(final String entityName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SimpleNaturalIdLoadAccess bySimpleNaturalId(final Class entityClass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Filter enableFilter(final String filterName) {
        final EnableFilterOpenSessionEvent event = new EnableFilterOpenSessionEvent(filterName);
        for (final Shard shard : shards) {
            if (shard.getSession() != null) {
                shard.getSession().enableFilter(filterName);
            } else {
                shard.addOpenSessionEvent(event);
            }
        }
        // TODO(maxr) what do we return here?  A sharded filter?
        return null;
    }

    @Override
    public Filter getEnabledFilter(final String filterName) {
        // all session have same filters
        for (final Shard shard : shards) {
            if (shard.getSession() != null) {
                final Filter filter = shard.getSession().getEnabledFilter(filterName);
                if (filter != null) {
                    return filter;
                }
            }
        }
        // TODO(maxr) what do we return here?
        return null;
    }

    @Override
    public void disableFilter(final String filterName) {
        final DisableFilterOpenSessionEvent event = new DisableFilterOpenSessionEvent(filterName);
        for (Shard shard : shards) {
            if (shard.getSession() != null) {
                shard.getSession().disableFilter(filterName);
            } else {
                shard.addOpenSessionEvent(event);
            }
        }
    }

    @Override
    public SessionStatistics getStatistics() {
        return new ShardedSessionStatistics(this);
    }

    @Override
    public boolean isReadOnly(final Object entityOrProxy) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setReadOnly(final Object entity, final boolean readOnly) {
        final SetReadOnlyOpenSessionEvent event = new SetReadOnlyOpenSessionEvent(entity, readOnly);
        for (final Shard shard : shards) {
            if (shard.getSession() != null) {
                shard.getSession().setReadOnly(entity, readOnly);
            } else {
                shard.addOpenSessionEvent(event);
            }
        }
    }

    @Override
    public void doWork(final Work work) throws HibernateException {
        for (final Shard shard : shards) {
            shard.getSession().doWork(work);
        }
    }

    @Override
    public <T> T doReturningWork(final ReturningWork<T> work) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Connection disconnect() throws HibernateException {
        for (final Shard s : getShards()) {
            s.getSession().disconnect();
        }
        // we do not allow application-supplied connections, so we can always return
        // null
        return null;
    }

    /**
     * Unsupported.  This is a technical decision.
     */
    @Override
    public void reconnect(final Connection connection) throws HibernateException {
        throw new UnsupportedOperationException("Cannot reconnect a sharded session");
    }

    @Override
    public boolean isFetchProfileEnabled(final String name) throws UnknownProfileException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void enableFetchProfile(final String name) throws UnknownProfileException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void disableFetchProfile(final String name) throws UnknownProfileException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TypeHelper getTypeHelper() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public LobHelper getLobHelper() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Session getSessionForObject(final Object obj) {
        return getSessionForObject(obj, shards);
    }

    @Override
    public ShardId getShardIdForObject(final Object obj, final List<Shard> shardsToConsider) {
        // TODO(maxr) optimize this by keeping an identity map of objects to shardId
        Shard shard = getShardForObject(obj, shardsToConsider);
        if (shard == null) {
            return null;
        } else if (shard.getShardIds().size() == 1) {
            return shard.getShardIds().iterator().next();
        } else {
            String className;
            if (obj instanceof HibernateProxy) {
                className = ((HibernateProxy) obj).getHibernateLazyInitializer().getPersistentClass().getName();
            } else {
                className = obj.getClass().getName();
            }
            IdentifierGenerator idGenerator = shard.getSessionFactoryImplementor().getIdentifierGenerator(className);
            if (idGenerator instanceof ShardEncodingIdentifierGenerator) {
                return ((ShardEncodingIdentifierGenerator) idGenerator).extractShardId(getIdentifier(obj));
            } else {
                // TODO(tomislav): also use shard resolution strategy if it returns only 1 shard; throw this error in config instead of here
                throw new HibernateException("Can not use virtual sharding with non-shard resolving id gen");
            }
        }
    }

    @Override
    public ShardId getShardIdForObject(final Object obj) {
        return getShardIdForObject(obj, shards);
    }

    @Override
    public void lockShard() {
        lockedShard = true;
    }

    public boolean getCheckAllAssociatedObjectsForDifferentShards() {
        return checkAllAssociatedObjectsForDifferentShards;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (!closed) {
                LOG.shardedSessionImplIsBeingGCButWasNotClosedProperly();
                try {
                    close();
                } catch (Exception e) {
                    LOG.caughtExceptionWhenTryingToClose();
                }
            }
        } finally {
            super.finalize();
        }
    }

    public static ShardId getCurrentSubgraphShardId() {
        return currentSubgraphShardId.get();
    }

    public static void setCurrentSubgraphShardId(final ShardId shardId) {
        currentSubgraphShardId.set(shardId);
    }

    private Shard getShardForObject(final Object obj, final List<Shard> shardsToConsider) {
        for (final Shard shard : shardsToConsider) {
            if (shard.getSession() != null && shard.getSession().contains(obj)) {
                return shard;
            }
        }
        return null;
    }

    private Session getSessionForObject(final Object obj, final List<Shard> shardsToConsider) {
        final Shard shard = getShardForObject(obj, shardsToConsider);
        if (shard == null) {
            return null;
        }
        return shard.getSession();
    }

    private Shard getShardForCollection(final Object coll, final List<Shard> shardsToConsider) {
        for (final Shard shard : shardsToConsider) {
            if (shard.getSession() != null) {
                final SessionImplementor si = ((SessionImplementor) shard.getSession());
                if (si.getPersistenceContext().getCollectionEntryOrNull(coll) != null) {
                    return shard;
                }
            }
        }
        return null;
    }

    private Map<ShardId, Shard> buildShardIdsToShardsMap() {
        final Map<ShardId, Shard> map = Maps.newHashMap();
        for (final Shard shard : shards) {
            for (ShardId shardId : shard.getShardIds()) {
                map.put(shardId, shard);
            }
        }
        return map;
    }

    private Object applyGetOperation(final ShardOperation<Object> shardOp,
                                     final ShardResolutionStrategyData srsd) {

        final List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(srsd);
        return shardStrategy.getShardAccessStrategy().apply(
                shardIdListToShardList(shardIds),
                shardOp,
                new FirstNonNullResultExitStrategy<Object>(),
                new ExitOperationsQueryCollector());
    }

    private List<Shard> shardIdListToShardList(List<ShardId> shardIds) {
        final Set<Shard> shards = Collections.emptySet();
        for (final ShardId shardId : shardIds) {
            shards.add(shardIdsToShards.get(shardId));
        }
        return Lists.newArrayList(shards);
    }

    private Session getSomeSession() {
        for (final Shard shard : shards) {
            if (shard.getSession() != null) {
                return shard.getSession();
            }
        }
        return null;
    }

    /**
     * We already know that we don't have a shardId locked in for this session,
     * and we already know that this object can't grab its session from some
     * other object (we looked).  If this class is in the set of classes
     * that don't support top-level saves, it's an error.
     * This is to prevent clients from accidentally splitting their object graphs
     * across multiple shards.
     */
    private void checkForUnsupportedTopLevelSave(final Class<?> clazz) {
        if (classesWithoutTopLevelSaveSupport.contains(clazz)) {
            LOG.attemptToSaveObjectAsTopLevelObject(clazz.getName());
            throw new HibernateException(
                    String.format("Attempt to save object of type %s as a top-level object.", clazz.getName()));
        }
    }

    List<ShardId> selectShardIdsFromShardResolutionStrategyData(final ShardResolutionStrategyData srsd) {
        final IdentifierGenerator idGenerator = shardedSessionFactory.getIdentifierGenerator(srsd.getEntityName());
        if ((idGenerator instanceof ShardEncodingIdentifierGenerator) && (srsd.getId() != null)) {
            return Collections.singletonList(((ShardEncodingIdentifierGenerator) idGenerator).extractShardId(srsd.getId()));
        }
        return shardStrategy.getShardResolutionStrategy().selectShardIdsFromShardResolutionStrategyData(srsd);
    }

    /**
     * Helper method we can use when we need to find the Shard with which a
     * specified object is associated and invoke the method on that Shard.
     * If the object isn't associated with a Session we just invoke it on a
     * random Session with the expectation that this will cause an error.
     */
    <T> T invokeOnShardWithObject(final ShardOperation<T> so, final Object object) throws HibernateException {
        final ShardId shardId = getShardIdForObject(object);
        Shard shardToUse;
        if (shardId == null) {
            // just ask this question of a random shard so we get the proper error
            shardToUse = shards.get(0);
        } else {
            shardToUse = shardIdsToShards.get(shardId);
        }
        return so.execute(shardToUse);
    }

    ShardId selectShardIdForNewObject(Object obj) {
        if (lockedShardId != null) {
            return lockedShardId;
        }
        ShardId shardId;
    /*
     * Someone is trying to save this object, and that's wonderful, but if
     * this object references or is referenced by any other objects that have already been
     * associated with a session it's important that this object end up
     * associated with the same session.  In order to make sure that happens,
     * we're going to look at the metadata for this object and see what
     * references we have, and then use those to determine the proper shard.
     * If we can't find any references we'll leave it up to the shard selection
     * strategy.
     */
        shardId = getShardIdOfRelatedObject(obj);
        if (shardId == null) {
            checkForUnsupportedTopLevelSave(obj.getClass());
            shardId = shardStrategy.getShardSelectionStrategy().selectShardIdForNewObject(obj);
        }
        // lock has been requested but shard has not yet been selected - lock it in
        if (lockedShard) {
            lockedShardId = shardId;
        }
        LOG.debugf("Selected shard %d for object of type %s", shardId.getId(), obj.getClass().getName());
        return shardId;
    }

    /**
     * TODO(maxr) I can see this method benefitting from a cache that lets us quickly
     * see which properties we might need to look at.
     */
    ShardId getShardIdOfRelatedObject(Object obj) {
        ClassMetadata cmd = getClassMetadata(obj.getClass());
        Type[] types = cmd.getPropertyTypes();
        Object[] values = cmd.getPropertyValues(obj);
        ShardId shardId = null;
        List<Collection<Object>> collections = null;
        for (Pair<Type, Object> pair : CrossShardRelationshipDetectingInterceptor.buildListOfAssociations(types, values)) {
            if (pair.getFirst().isCollectionType()) {
                /**
                 * collection types are more expensive to evaluate (might involve
                 * lazy-loading the contents of the collection from the db), so
                 * let's hold off until the end on the chance that we can fail
                 * quickly.
                 */
                if (collections == null) {
                    collections = Lists.newArrayList();
                }
                @SuppressWarnings("unchecked")
                Collection<Object> coll = (Collection<Object>) pair.getSecond();
                collections.add(coll);
            } else {
                shardId = checkForConflictingShardId(shardId, obj.getClass(), pair.getSecond());
                /**
                 * if we're not checking for different shards, return as soon as we've
                 * got one
                 */
                if (shardId != null && !checkAllAssociatedObjectsForDifferentShards) {
                    return shardId;
                }
            }
        }
        if (collections != null) {
            for (Object collEntry : Iterables.concat(collections)) {
                shardId = checkForConflictingShardId(shardId, obj.getClass(), collEntry);
                if (shardId != null && !checkAllAssociatedObjectsForDifferentShards) {
                    /**
                     * if we're not checking for different shards, return as soon as we've
                     * got one
                     */
                    return shardId;
                }
            }
        }
        return shardId;
    }

    ShardId checkForConflictingShardId(ShardId existingShardId, Class<?> newObjectClass, Object associatedObject) {
        ShardId localShardId = getShardIdForObject(associatedObject);
        if (localShardId != null) {
            if (existingShardId == null) {
                existingShardId = localShardId;
            } else if (!localShardId.equals(existingShardId)) {
                final String msg = String.format(
                        "Object of type %s is on shard %d but an associated object of type %s is on shard %d.",
                        newObjectClass.getName(),
                        existingShardId.getId(),
                        associatedObject.getClass().getName(),
                        localShardId.getId());
                LOG.error(msg);
                throw new CrossShardAssociationException(msg);
            }
        }
        return existingShardId;
    }

    ClassMetadata getClassMetadata(Class<?> clazz) {
        return getSessionFactory().getClassMetadata(clazz);
    }

    static List<Shard> buildShardListFromSessionFactoryShardIdMap(
            final Map<SessionFactoryImplementor, Set<ShardId>> sessionFactoryShardIdMap,
            final boolean checkAllAssociatedObjectsForDifferentShards,
            final ShardIdResolver shardIdResolver,
            final /*@Nullable*/ Interceptor interceptor) {

        final List<Shard> list = Collections.emptyList();

        for (final Map.Entry<SessionFactoryImplementor, Set<ShardId>> entry : sessionFactoryShardIdMap.entrySet()) {
            OpenSessionEvent eventToRegister = null;
            Interceptor interceptorToSet = interceptor;
            if (checkAllAssociatedObjectsForDifferentShards) {
                // cross shard association checks for updates are handled using interceptors
                CrossShardRelationshipDetectingInterceptor csrdi = new CrossShardRelationshipDetectingInterceptor(shardIdResolver);
                if (interceptorToSet == null) {
                    // no interceptor to wrap so just use the cross-shard detecting interceptor raw
                    // this is safe because it's a stateless interceptor
                    interceptorToSet = csrdi;
                } else {
                    // user specified their own interceptor, so wrap it with a decorator
                    // that will still do the cross shard association checks
                    final Pair<Interceptor, OpenSessionEvent> result = decorateInterceptor(csrdi, interceptor);
                    interceptorToSet = result.first;
                    eventToRegister = result.second;
                }
            } else if (interceptorToSet != null) {
                // user specified their own interceptor so need to account for the fact
                // that it might be stateful
                final Pair<Interceptor, OpenSessionEvent> result = handleStatefulInterceptor(interceptorToSet);
                interceptorToSet = result.first;
                eventToRegister = result.second;
            }

            final Shard shard = new ShardImpl(entry.getValue(), entry.getKey(), interceptorToSet);
            list.add(shard);

            if (eventToRegister != null) {
                shard.addOpenSessionEvent(eventToRegister);
            }
        }

        return list;
    }

    static Pair<Interceptor, OpenSessionEvent> handleStatefulInterceptor(Interceptor mightBeStateful) {
        OpenSessionEvent openSessionEvent = null;
        if (mightBeStateful instanceof StatefulInterceptorFactory) {
            mightBeStateful = ((StatefulInterceptorFactory) mightBeStateful).newInstance();
            if (mightBeStateful instanceof RequiresSession) {
                openSessionEvent = new SetSessionOnRequiresSessionEvent((RequiresSession) mightBeStateful);
            }
        }

        return Pair.of(mightBeStateful, openSessionEvent);
    }

    static Pair<Interceptor, OpenSessionEvent> decorateInterceptor(final CrossShardRelationshipDetectingInterceptor csrdi,
                                                                   final Interceptor decorateMe) {
        final Pair<Interceptor, OpenSessionEvent> pair = handleStatefulInterceptor(decorateMe);
        final Interceptor decorator = new CrossShardRelationshipDetectingInterceptorDecorator(csrdi, pair.first);
        return Pair.of(decorator, pair.second);
    }

    @Override
    public <T> T execute(Callback<T> callback) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    private void applyRefreshOperation(RefreshOperation op, Object object) {
        ShardId shardId = getShardIdForObject(object);
        if (shardId != null) {
            op.refresh(shardIdsToShards.get(shardId), object);
        } else {
            List<Shard> candidateShards = determineShardsObjectViaResolutionStrategy(object);
            if (candidateShards.size() == 1) {
                op.refresh(candidateShards.get(0), object);
            } else {
                for (Shard shard : candidateShards) {
                    try {
                        op.refresh(shard, object);
                        return;
                    } catch (UnresolvableObjectException uoe) {
                        // ignore
                    }
                }
                op.refresh(shards.get(0), object);
            }
        }
    }


    void errorIfClosed() {
        if (closed) {
            throw new SessionException("Session is closed!");
        }
    }


    void applySaveOrUpdateOperation(final SaveOrUpdateOperation op, final Object object) {
        ShardId shardId = getShardIdForObject(object);
        if (shardId != null) {
            // attached object
            op.saveOrUpdate(shardIdsToShards.get(shardId), object);
            return;
        }
        final List<Shard> potentialShards = determineShardsObjectViaResolutionStrategy(object);
        if (potentialShards.size() == 1) {
            op.saveOrUpdate(potentialShards.get(0), object);
            return;
        }

        /**
         * Too bad, we've got a detached object that could be on more than 1 shard.
         * The only safe way to handle this is to try and lookup the object, and if
         * it exists, do a merge, and if it doesn't, do a save.
         */
        final Serializable id = extractId(object);
        if (id != null) {
            Object persistent = get(object.getClass(), id);
            if (persistent != null) {
                shardId = getShardIdForObject(persistent);
            }
        }
        if (shardId != null) {
            op.merge(shardIdsToShards.get(shardId), object);
        } else {
            save(object);
        }
    }

    Serializable extractId(final Object object) {
        final ClassMetadata cmd = shardedSessionFactory.getClassMetadata(object.getClass());
        // I'm just guessing about the EntityMode
        return cmd.getIdentifier(object, this);
    }


    private void applyUpdateOperation(final UpdateOperation op, final Object object) {
        ShardId shardId = getShardIdForObject(object);
        if (shardId != null) {
            // attached object
            op.update(shardIdsToShards.get(shardId), object);
            return;
        }
        final List<Shard> potentialShards = determineShardsObjectViaResolutionStrategy(object);
        if (potentialShards.size() == 1) {
            op.update(potentialShards.get(0), object);
            return;
        }
        /**
         * Too bad, we've got a detached object that could be on more than 1 shard.
         * The only safe way to perform the update is to load the object and then
         * do a merge.
         */
        Serializable id = extractId(object);
        if (id != null) {
            Object persistent = get(object.getClass(), extractId(object));
            if (persistent != null) {
                shardId = getShardIdForObject(persistent);
            }
        }
        if (shardId == null) {
            /**
             * This is an error condition.  In order to provide the same behavior
             * as a non-sharded session we're just going to dispatch the update
             * to a random shard (we know it will fail because either we don't have
             * an id or the lookup returned).
             */
            op.update(getShards().get(0), object);
            // this call may succeed but the commit will fail
        } else {
            op.merge(shardIdsToShards.get(shardId), object);
        }
    }

    List<Shard> determineShardsObjectViaResolutionStrategy(final Object object) {
        final Serializable id = extractId(object);
        if (id == null) {
            return Collections.emptyList();
        }
        final ShardResolutionStrategyData srsd = new ShardResolutionStrategyDataImpl(object.getClass(), id);
        final List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(srsd);
        return shardIdListToShardList(shardIds);
    }

    private void applyDeleteOperation(DeleteOperation op, Object object) {
        ShardId shardId = getShardIdForObject(object);
        if (shardId != null) {
            // attached object
            op.delete(shardIdsToShards.get(shardId), object);
            return;
        }
        /**
         * Detached object.
         * We can't just try to delete on each shard because if you have an
         * object associated with Session x and you try to delete that object in
         * Session y, and if that object has persistent collections, Hibernate will
         * blow up because it will try to associate the persistent collection with
         * a different Session as part of the cascade.  In order to avoid this we
         * need to be precise about the shard on which we perform the delete.
         *
         * First let's see if we can derive the shard just from the object's id.
         */
        List<Shard> potentialShards = determineShardsObjectViaResolutionStrategy(object);
        if (potentialShards.size() == 1) {
            op.delete(potentialShards.get(0), object);
            return;
        }
        /**
         * Too bad, we've got a detached object that could be on more than 1 shard.
         * The only safe way to perform the delete is to load the object before
         * deleting.
         */
        Object persistent = get(object.getClass(), extractId(object));
        shardId = getShardIdForObject(persistent);
        op.delete(shardIdsToShards.get(shardId), persistent);
    }

    private interface RefreshOperation {
        void refresh(Shard shard, Object object);
    }

    private interface UpdateOperation {
        void update(Shard shard, Object object);
        void merge(Shard shard, Object object);
    }

    interface SaveOrUpdateOperation {
        void saveOrUpdate(Shard shard, Object object);
        void merge(Shard shard, Object object);
    }

    private interface DeleteOperation {
        void delete(Shard shard, Object object);
    }

    private static class DefaultRefreshOperation implements RefreshOperation {

        @Override
        public void refresh(final Shard shard, final Object object) {
            shard.establishSession().refresh(object);
        }
    }

    private static class SimpleUpdateOperation implements UpdateOperation {

        @Override
        public void update(Shard shard, Object object) {
            shard.establishSession().update(object);
        }

        @Override
        public void merge(Shard shard, Object object) {
            shard.establishSession().merge(object);
        }
    }

    private static class EntityNameUpdateOperation implements UpdateOperation {

        private final String entityName;

        public EntityNameUpdateOperation(final String entityName) {
            this.entityName = entityName;
        }

        @Override
        public void update(final Shard shard, final Object object) {
            shard.establishSession().update(entityName, object);
        }

        @Override
        public void merge(final Shard shard, final Object object) {
            shard.establishSession().merge(entityName, object);
        }
    }

    private static class SimpleSaveOrUpdateOperation implements SaveOrUpdateOperation {

        @Override
        public void saveOrUpdate(final Shard shard, final Object object) {
            shard.establishSession().update(object);
        }

        @Override
        public void merge(final Shard shard, final Object object) {
            shard.establishSession().merge(object);
        }
    }

    private static class EntityNameSaveOrUpdateOperation implements SaveOrUpdateOperation {

        private final String entityName;

        public EntityNameSaveOrUpdateOperation(final String entityName) {
            this.entityName = entityName;
        }

        @Override
        public void saveOrUpdate(Shard shard, Object object) {
            shard.establishSession().saveOrUpdate(entityName, object);
        }

        @Override
        public void merge(Shard shard, Object object) {
            shard.establishSession().merge(entityName, object);
        }
    }

    private static class SimpleDeleteOperation implements DeleteOperation {

        @Override
        public void delete(final Shard shard, final Object object) {
            shard.establishSession().delete(object);
        }
    }

    private static class EntityNameDeleteOperation implements DeleteOperation {

        private final String entityName;

        public EntityNameDeleteOperation(final String entityName) {
            this.entityName = entityName;
        }

        @Override
        public void delete(Shard shard, Object object) {
            shard.establishSession().delete(entityName, object);
        }
    }
}

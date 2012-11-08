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

import org.hibernate.Cache;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.TypeHelper;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.UpdateTimestampsCache;
import org.hibernate.cfg.Settings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.query.spi.QueryPlanCache;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.shards.ShardId;
import org.hibernate.shards.engine.ShardedSessionFactoryImplementor;
import org.hibernate.shards.id.GeneratorRequiringControlSessionProvider;
import org.hibernate.shards.internal.ShardsMessageLogger;
import org.hibernate.shards.strategy.ShardStrategy;
import org.hibernate.shards.strategy.ShardStrategyFactory;
import org.hibernate.shards.util.Iterables;
import org.hibernate.shards.util.Lists;
import org.hibernate.shards.util.Maps;
import org.hibernate.shards.util.Preconditions;
import org.hibernate.shards.util.Sets;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.internal.ConcurrentStatisticsImpl;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;
import org.jboss.logging.Logger;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.transaction.TransactionManager;
import java.io.Serializable;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Shard-aware implementation of {@link SessionFactory}.
 *
 * @author maxr@google.com (Max Ross)
 */
public class ShardedSessionFactoryImpl implements ShardedSessionFactoryImplementor, ControlSessionProvider {

    public static final ShardsMessageLogger LOG = Logger.getMessageLogger(ShardsMessageLogger.class, ShardedSessionFactoryImpl.class.getName());

    // the id of the control shard
    private static final int CONTROL_SHARD_ID = 0;

    // the SessionFactoryImplementor objects to which we delegate
    private final List<SessionFactoryImplementor> sessionFactories;

    // All classes that cannot be directly saved
    private final Set<Class<?>> classesWithoutTopLevelSaveSupport;

    // map of SessionFactories used by this ShardedSessionFactory (might be a subset of all SessionFactories)
    private final Map<SessionFactoryImplementor, Set<ShardId>> sessionFactoryShardIdMap;

    // map of all existing SessionFactories, used when creating a new ShardedSessionFactory for some subset of shards
    private final Map<SessionFactoryImplementor, Set<ShardId>> fullSessionFactoryShardIdMap;

    // The strategy we use for all shard-related operations
    private final ShardStrategy shardStrategy;

    // Reference to the SessionFactory we use for functionality that expects
    // data to live in a single, well-known location (like distributed sequences)
    private final SessionFactoryImplementor controlSessionFactory;

    // flag to indicate whether we should do full cross-shard relationship
    // checking (very slow)
    private final boolean checkAllAssociatedObjectsForDifferentShards;

    // Statistics aggregated across all contained SessionFactories
    private final Statistics statistics = new ConcurrentStatisticsImpl(this);

    /**
     * Constructs a ShardedSessionFactoryImpl
     *
     * @param shardIds                 The ids of the shards with which this SessionFactory
     *                                 should be associated.
     * @param sessionFactoryShardIdMap Mapping of SessionFactories to shard ids.
     *                                 When using virtual shards, this map associates SessionFactories (physical
     *                                 shards) with virtual shards (shard ids).  Map cannot be empty.
     *                                 Map keys cannot be null.  Map values cannot be null or empty.
     * @param shardStrategyFactory     factory that knows how to create the {@link ShardStrategy}
     *                                 that will be used for all shard-related operations
     * @param classesWithoutTopLevelSaveSupport
     *                                 All classes that cannot be saved
     *                                 as top-level objects
     * @param checkAllAssociatedObjectsForDifferentShards
     *                                 Flag that controls
     *                                 whether or not we do full cross-shard relationshp checking (very slow)
     */
    public ShardedSessionFactoryImpl(final List<ShardId> shardIds,
                                     final Map<SessionFactoryImplementor, Set<ShardId>> sessionFactoryShardIdMap,
                                     final ShardStrategyFactory shardStrategyFactory,
                                     final Set<Class<?>> classesWithoutTopLevelSaveSupport,
                                     final boolean checkAllAssociatedObjectsForDifferentShards) {

        Preconditions.checkNotNull(sessionFactoryShardIdMap);
        Preconditions.checkArgument(!sessionFactoryShardIdMap.isEmpty());
        Preconditions.checkNotNull(shardStrategyFactory);
        Preconditions.checkNotNull(classesWithoutTopLevelSaveSupport);

        this.sessionFactories = Lists.newArrayList(sessionFactoryShardIdMap.keySet());
        this.sessionFactoryShardIdMap = Maps.newHashMap();
        this.fullSessionFactoryShardIdMap = sessionFactoryShardIdMap;
        this.classesWithoutTopLevelSaveSupport = Sets.newHashSet(classesWithoutTopLevelSaveSupport);
        this.checkAllAssociatedObjectsForDifferentShards = checkAllAssociatedObjectsForDifferentShards;
        Set<ShardId> uniqueShardIds = Sets.newHashSet();
        SessionFactoryImplementor controlSessionFactoryToSet = null;
        for (Map.Entry<SessionFactoryImplementor, Set<ShardId>> entry : sessionFactoryShardIdMap.entrySet()) {
            SessionFactoryImplementor implementor = entry.getKey();
            Preconditions.checkNotNull(implementor);
            Set<ShardId> shardIdSet = entry.getValue();
            Preconditions.checkNotNull(shardIdSet);
            Preconditions.checkArgument(!shardIdSet.isEmpty());
            for (ShardId shardId : shardIdSet) {
                // TODO(tomislav): we should change it so we specify control shard in configuration
                if (shardId.getId() == CONTROL_SHARD_ID) {
                    controlSessionFactoryToSet = implementor;
                }
                if (!uniqueShardIds.add(shardId)) {
                    LOG.cannotHaveMoreThanOneShardWithSameId(shardId.getId());
                    throw new HibernateException(String.format("Cannot have more than one shard with shard id %d.", shardId.getId()));
                }
                if (shardIds.contains(shardId)) {
                    if (!this.sessionFactoryShardIdMap.containsKey(implementor)) {
                        this.sessionFactoryShardIdMap.put(implementor, Sets.<ShardId>newHashSet());
                    }
                    this.sessionFactoryShardIdMap.get(implementor).add(shardId);
                }
            }
        }
        // make sure someone didn't associate a session factory with a shard id
        // that isn't in the full list of shards
        for (ShardId shardId : shardIds) {
            Preconditions.checkState(uniqueShardIds.contains(shardId));
        }
        controlSessionFactory = controlSessionFactoryToSet;
        // now that we have all our shard ids, construct our shard strategy
        this.shardStrategy = shardStrategyFactory.newShardStrategy(shardIds);
        setupIdGenerators();
    }

    /**
     * Constructs a ShardedSessionFactoryImpl
     *
     * @param sessionFactoryShardIdMap Mapping of SessionFactories to shard ids.
     *                                 When using virtual shards, this map associates SessionFactories (physical
     *                                 shards) with virtual shards (shard ids).  Map cannot be empty.
     *                                 Map keys cannot be null.  Map values cannot be null or empty.
     * @param shardStrategyFactory     factory that knows how to create the {@link ShardStrategy}
     *                                 that will be used for all shard-related operations
     * @param classesWithoutTopLevelSaveSupport
     *                                 All classes that cannot be saved
     *                                 as top-level objects
     * @param checkAllAssociatedObjectsForDifferentShards
     *                                 Flag that controls
     *                                 whether or not we do full cross-shard relationshp checking (very slow)
     */
    public ShardedSessionFactoryImpl(
            Map<SessionFactoryImplementor, Set<ShardId>> sessionFactoryShardIdMap,
            ShardStrategyFactory shardStrategyFactory,
            Set<Class<?>> classesWithoutTopLevelSaveSupport,
            boolean checkAllAssociatedObjectsForDifferentShards) {
        this(Lists.newArrayList(Iterables.concat(sessionFactoryShardIdMap.values())),
                sessionFactoryShardIdMap,
                shardStrategyFactory,
                classesWithoutTopLevelSaveSupport,
                checkAllAssociatedObjectsForDifferentShards);
    }

    /**
     * Sets the {@link ControlSessionProvider} on id generators that implement the
     * {@link GeneratorRequiringControlSessionProvider} interface
     */
    private void setupIdGenerators() {
        for (SessionFactoryImplementor sfi : sessionFactories) {
            for (Object obj : sfi.getAllClassMetadata().values()) {
                ClassMetadata cmd = (ClassMetadata) obj;
                EntityPersister ep = sfi.getEntityPersister(cmd.getEntityName());
                if (ep.getIdentifierGenerator() instanceof GeneratorRequiringControlSessionProvider) {
                    ((GeneratorRequiringControlSessionProvider) ep.getIdentifierGenerator()).setControlSessionProvider(this);
                }
            }
        }
    }

    @Override
    public Map<SessionFactoryImplementor, Set<ShardId>> getSessionFactoryShardIdMap() {
        return sessionFactoryShardIdMap;
    }

    /**
     * Warning: this interceptor will be shared across all shards, so be very
     * careful about using a stateful implementation.
     */
    @Override
    public ShardedSession openSession(final Interceptor interceptor) throws HibernateException {
        return new ShardedSessionImpl(
                interceptor,
                this,
                shardStrategy,
                classesWithoutTopLevelSaveSupport,
                checkAllAssociatedObjectsForDifferentShards);
    }

    @Override
    public ShardedSession openSession() throws HibernateException {
        return new ShardedSessionImpl(
                this,
                shardStrategy,
                classesWithoutTopLevelSaveSupport,
                checkAllAssociatedObjectsForDifferentShards);
    }

    /**
     * Unsupported.  This is a project decision.  We'll get to it later.
     */
    @Override
    public StatelessSession openStatelessSession() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Unsupported.  This is a technical decision.  See {@link ShardedSessionFactoryImpl#openSession(Connection)}
     * for an explanation.
     */
    @Override
    public StatelessSession openStatelessSession(final Connection connection) {
        throw new UnsupportedOperationException(
                "Cannot open a stateless sharded session with a user provided connection");
    }

    @Override
    public Session getCurrentSession() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public StatelessSessionBuilder withStatelessOptions() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ClassMetadata getClassMetadata(final Class persistentClass) throws HibernateException {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getClassMetadata(persistentClass);
    }

    @Override
    public ClassMetadata getClassMetadata(final String entityName) throws HibernateException {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getClassMetadata(entityName);
    }

    @Override
    public CollectionMetadata getCollectionMetadata(final String roleName) throws HibernateException {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getCollectionMetadata(roleName);
    }

    @Override
    public Map<String, ClassMetadata> getAllClassMetadata() throws HibernateException {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getAllClassMetadata();
    }

    @Override
    public Map getAllCollectionMetadata() throws HibernateException {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getAllCollectionMetadata();
    }

    /**
     * Unsupported.  This is a scope decision, not a technical decision.
     */
    @Override
    public Statistics getStatistics() {
        return statistics;
    }

    @Override
    public void close() throws HibernateException {
        for (final SessionFactory sf : sessionFactories) {
            sf.close();
        }
        sessionFactories.clear();
        if (classesWithoutTopLevelSaveSupport != null) {
            classesWithoutTopLevelSaveSupport.clear();
        }
        if (sessionFactoryShardIdMap != null) {
            sessionFactoryShardIdMap.clear();
        }
        if (fullSessionFactoryShardIdMap != null) {
            fullSessionFactoryShardIdMap.clear();
        }
        statistics.clear();
    }

    @Override
    public boolean isClosed() {
        // a ShardedSessionFactory is closed if any of its SessionFactories are closed
        for (final SessionFactory sf : sessionFactories) {
            if (sf.isClosed()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Cache getCache() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @Deprecated
    public void evict(final Class persistentClass) throws HibernateException {
        for (SessionFactory sf : sessionFactories) {
            sf.getCache().evictEntityRegion(persistentClass);
        }
    }

    @Override
    @Deprecated
    public void evict(final Class persistentClass, final Serializable id) throws HibernateException {
        for (SessionFactory sf : sessionFactories) {
            sf.getCache().evictEntity(persistentClass, id);
        }
    }

    @Override
    @Deprecated
    public void evictEntity(final String entityName) throws HibernateException {
        for (SessionFactory sf : sessionFactories) {
            sf.getCache().evictEntityRegion(entityName);
        }
    }

    @Override
    @Deprecated
    public void evictEntity(final String entityName, final Serializable id) throws HibernateException {
        for (SessionFactory sf : sessionFactories) {
            sf.getCache().evictEntity(entityName, id);
        }
    }

    @Override
    @Deprecated
    public void evictCollection(final String roleName) throws HibernateException {
        for (final SessionFactory sf : sessionFactories) {
            sf.getCache().evictCollectionRegion(roleName);
        }
    }

    @Override
    @Deprecated
    public void evictCollection(final String roleName, final Serializable id) throws HibernateException {
        for (SessionFactory sf : sessionFactories) {
            sf.getCache().evictCollection(roleName, id);
        }
    }

    @Override
    @Deprecated
    public void evictQueries() throws HibernateException {
        for (final SessionFactory sf : sessionFactories) {
            sf.getCache().evictEntityRegions();
        }
    }

    @Override
    @Deprecated
    public void evictQueries(final String cacheRegion) throws HibernateException {
        for (final SessionFactory sf : sessionFactories) {
            sf.getCache().evictQueryRegion(cacheRegion);
        }
    }

    @Override
    public Set getDefinedFilterNames() {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getDefinedFilterNames();
    }

    @Override
    public FilterDefinition getFilterDefinition(final String filterName) throws HibernateException {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getFilterDefinition(filterName);
    }

    @Override
    public boolean containsFetchProfileDefinition(String name) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TypeHelper getTypeHelper() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Unsupported.  This is a scope decision, not a technical one.
     */
    @Override
    public Reference getReference() throws NamingException {
        throw new UnsupportedOperationException(
                "Sharded session factories do not support References (sorry).");
    }

    @Override
    public IdentifierGenerator getIdentifierGenerator(final String rootEntityName) {
        // since all configs are same, we return any
        return getAnyFactory().getIdentifierGenerator(rootEntityName);
    }

    @Override
    public SessionImplementor openControlSession() {
        Preconditions.checkState(controlSessionFactory != null);
        Session session = controlSessionFactory.openSession();
        return (SessionImplementor) session;
    }

    @Override
    public boolean containsFactory(final SessionFactoryImplementor factory) {
        return sessionFactories.contains(factory);
    }

    private SessionFactoryImplementor getAnyFactory() {
        return sessionFactories.get(0);
    }

    @Override
    public List<SessionFactory> getSessionFactories() {
        return Collections.<SessionFactory>unmodifiableList(sessionFactories);
    }

    /**
     * Constructs a ShardedSessionFactory that operates on the given list of
     * shardIds. This operation is relatively lightweight as the returned
     * ShardedSessionFactory reuses existing shards. Most common use will be to
     * provide a ShardedSessionFactory that manages a subset of the application's
     * shards.
     *
     * @param shardIds
     * @param shardStrategyFactory
     * @return ShardedSessionFactory
     */
    @Override
    public ShardedSessionFactory getSessionFactory(final List<ShardId> shardIds,
                                                   final ShardStrategyFactory shardStrategyFactory) {
        return new SubsetShardedSessionFactoryImpl(shardIds,
                fullSessionFactoryShardIdMap,
                shardStrategyFactory,
                classesWithoutTopLevelSaveSupport,
                checkAllAssociatedObjectsForDifferentShards);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            // try to be helpful to apps that don't clean up properly
            if (!isClosed()) {
                LOG.shardedSessionFactoryImplIsBeingGCButWasNotClosedProperly();
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

    @Override
    public SessionFactoryOptions getSessionFactoryOptions() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SessionBuilderImplementor withOptions() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TypeResolver getTypeResolver() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Properties getProperties() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public EntityPersister getEntityPersister(String entityName) throws MappingException {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getEntityPersister(entityName);
    }

    @Override
    public Map<String, EntityPersister> getEntityPersisters() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public CollectionPersister getCollectionPersister(final String role) throws MappingException {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getCollectionPersister(role);
    }

    @Override
    public Map<String, CollectionPersister> getCollectionPersisters() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public JdbcServices getJdbcServices() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Dialect getDialect() {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getDialect();
    }

    @Override
    public Interceptor getInterceptor() {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getInterceptor();
    }

    @Override
    public QueryPlanCache getQueryPlanCache() {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getQueryPlanCache();
    }

    @Override
    public Type[] getReturnTypes(String queryString) throws HibernateException {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getReturnTypes(queryString);
    }

    @Override
    public String[] getReturnAliases(String queryString) throws HibernateException {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getReturnAliases(queryString);
    }

    @Override
    @Deprecated
    public ConnectionProvider getConnectionProvider() {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getConnectionProvider();
    }

    @Override
    public String[] getImplementors(final String className) throws MappingException {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getImplementors(className);
    }

    @Override
    public String getImportedClassName(final String name) {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getImportedClassName(name);
    }

    @Override
    public QueryCache getQueryCache() {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getQueryCache();
    }

    @Override
    public QueryCache getQueryCache(final String regionName) throws HibernateException {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getQueryCache(regionName);
    }

    @Override
    public UpdateTimestampsCache getUpdateTimestampsCache() {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getUpdateTimestampsCache();
    }

    @Override
    public StatisticsImplementor getStatisticsImplementor() {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getStatisticsImplementor();
    }

    @Override
    public NamedQueryDefinition getNamedQuery(String queryName) {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getNamedQuery(queryName);
    }

    @Override
    public NamedSQLQueryDefinition getNamedSQLQuery(final String queryName) {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getNamedSQLQuery(queryName);
    }

    @Override
    public ResultSetMappingDefinition getResultSetMapping(final String name) {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getResultSetMapping(name);
    }

    @Override
    public Region getSecondLevelCacheRegion(final String regionName) {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getSecondLevelCacheRegion(regionName);
    }

    @Override
    public Region getNaturalIdCacheRegion(String regionName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map getAllSecondLevelCacheRegions() {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getAllSecondLevelCacheRegions();
    }

    @Override
    public SQLExceptionConverter getSQLExceptionConverter() {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getSQLExceptionConverter();
    }

    @Override
    public SqlExceptionHelper getSQLExceptionHelper() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Settings getSettings() {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getSettings();
    }

    @Override
    public Session openTemporarySession() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getCollectionRolesByEntityParticipant(final String entityName) {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getCollectionRolesByEntityParticipant(entityName);
    }

    @Override
    public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Type getIdentifierType(final String className) throws MappingException {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getIdentifierType(className);
    }

    @Override
    public String getIdentifierPropertyName(final String className) throws MappingException {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getIdentifierPropertyName(className);
    }

    @Override
    public Type getReferencedPropertyType(final String className, final String propertyName) throws MappingException {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getReferencedPropertyType(className, propertyName);
    }

    @Override
    public EntityNotFoundDelegate getEntityNotFoundDelegate() {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getEntityNotFoundDelegate();
    }

    @Override
    public SQLFunctionRegistry getSqlFunctionRegistry() {
        // assumption is that all session factories are configured the same way,
        // so it doesn't matter which session factory answers this question
        return getAnyFactory().getSqlFunctionRegistry();
    }

    @Override
    public FetchProfile getFetchProfile(final String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ServiceRegistryImplementor getServiceRegistry() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addObserver(SessionFactoryObserver observer) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

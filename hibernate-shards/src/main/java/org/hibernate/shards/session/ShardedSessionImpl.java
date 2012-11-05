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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.ReplicationMode;
import org.hibernate.SQLQuery;
import org.hibernate.SessionException;
import org.hibernate.Transaction;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.classic.Session;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.metadata.ClassMetadata;
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
import org.hibernate.shards.query.AdHocQueryFactoryImpl;
import org.hibernate.shards.query.ExitOperationsQueryCollector;
import org.hibernate.shards.query.NamedQueryFactoryImpl;
import org.hibernate.shards.query.QueryId;
import org.hibernate.shards.query.ShardedQueryImpl;
import org.hibernate.shards.stat.ShardedSessionStatistics;
import org.hibernate.shards.strategy.ShardStrategy;
import org.hibernate.shards.strategy.exit.FirstNonNullResultExitStrategy;
import org.hibernate.shards.strategy.selection.ShardResolutionStrategyData;
import org.hibernate.shards.strategy.selection.ShardResolutionStrategyDataImpl;
import org.hibernate.shards.transaction.ShardedTransactionImpl;
import org.hibernate.shards.util.Iterables;
import org.hibernate.shards.util.Lists;
import org.hibernate.shards.util.Maps;
import org.hibernate.shards.util.Pair;
import org.hibernate.shards.util.Preconditions;
import org.hibernate.shards.util.Sets;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.type.Type;

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
public class ShardedSessionImpl implements ShardedSession, ShardedSessionImplementor,
    ShardIdResolver {

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

  private final Log log = LogFactory.getLog(getClass());

  /**
   * Constructor used for openSession(...) processing.
   *
   * @param shardedSessionFactory The factory from which this session was obtained
   * @param shardStrategy The shard strategy for this session
   * @param classesWithoutTopLevelSaveSupport The set of classes on which top-level save can not be performed
   * @param checkAllAssociatedObjectsForDifferentShards Should we check for cross-shard relationships
   */
  ShardedSessionImpl(
      ShardedSessionFactoryImplementor shardedSessionFactory,
      ShardStrategy shardStrategy,
      Set<Class<?>> classesWithoutTopLevelSaveSupport,
      boolean checkAllAssociatedObjectsForDifferentShards) {
    this(
        null,
        shardedSessionFactory,
        shardStrategy,
        classesWithoutTopLevelSaveSupport,
        checkAllAssociatedObjectsForDifferentShards);
  }

  /**
   * Constructor used for openSession(...) processing.
   *
   * @param interceptor The interceptor to be applied to this session
   * @param shardedSessionFactory The factory from which this session was obtained
   * @param shardStrategy The shard strategy for this session
   * @param classesWithoutTopLevelSaveSupport The set of classes on which top-level save can not be performed
   * @param checkAllAssociatedObjectsForDifferentShards Should we check for cross-shard relationships
   */
  ShardedSessionImpl(
      /*@Nullable*/ Interceptor interceptor,
      ShardedSessionFactoryImplementor shardedSessionFactory,
      ShardStrategy shardStrategy,
      Set<Class<?>> classesWithoutTopLevelSaveSupport,
      boolean checkAllAssociatedObjectsForDifferentShards) {
    this.shardedSessionFactory = shardedSessionFactory;
    this.shards =
        buildShardListFromSessionFactoryShardIdMap(
            shardedSessionFactory.getSessionFactoryShardIdMap(),
            checkAllAssociatedObjectsForDifferentShards,
            this,
            interceptor);
    this.shardIdsToShards = buildShardIdsToShardsMap();
    this.shardStrategy = shardStrategy;
    this.classesWithoutTopLevelSaveSupport = classesWithoutTopLevelSaveSupport;
    this.checkAllAssociatedObjectsForDifferentShards = checkAllAssociatedObjectsForDifferentShards;
  }

  private Map<ShardId, Shard> buildShardIdsToShardsMap() {
    Map<ShardId, Shard> map = Maps.newHashMap();
    for(Shard shard : shards) {
      for(ShardId shardId : shard.getShardIds()) {
        map.put(shardId, shard);
      }
    }
    return map;
  }

  static List<Shard> buildShardListFromSessionFactoryShardIdMap(
      Map<SessionFactoryImplementor, Set<ShardId>> sessionFactoryShardIdMap,
      boolean checkAllAssociatedObjectsForDifferentShards,
      ShardIdResolver shardIdResolver,
      /*@Nullable*/ final Interceptor interceptor) {
    List<Shard> list = Lists.newArrayList();
    for(Map.Entry<SessionFactoryImplementor, Set<ShardId>> entry : sessionFactoryShardIdMap.entrySet()) {
      OpenSessionEvent eventToRegister = null;
      Interceptor interceptorToSet = interceptor;
      if(checkAllAssociatedObjectsForDifferentShards) {
        // cross shard association checks for updates are handled using interceptors
        CrossShardRelationshipDetectingInterceptor csrdi = new CrossShardRelationshipDetectingInterceptor(shardIdResolver);
        if(interceptorToSet == null) {
          // no interceptor to wrap so just use the cross-shard detecting interceptor raw
          // this is safe because it's a stateless interceptor
          interceptorToSet = csrdi;
        } else {
          // user specified their own interceptor, so wrap it with a decorator
          // that will still do the cross shard association checks
          Pair<Interceptor, OpenSessionEvent> result = decorateInterceptor(csrdi, interceptor);
          interceptorToSet = result.first;
          eventToRegister = result.second;
        }
      } else if(interceptorToSet != null) {
        // user specified their own interceptor so need to account for the fact
        // that it might be stateful
        Pair<Interceptor, OpenSessionEvent> result = handleStatefulInterceptor(interceptorToSet);
        interceptorToSet = result.first;
        eventToRegister = result.second;
      }
      Shard shard =
          new ShardImpl(
              entry.getValue(),
              entry.getKey(),
              interceptorToSet);
      list.add(shard);
      if(eventToRegister != null) {
        shard.addOpenSessionEvent(eventToRegister);
      }
    }
    return list;
  }

  static Pair<Interceptor, OpenSessionEvent> handleStatefulInterceptor(
      Interceptor mightBeStateful) {
    OpenSessionEvent openSessionEvent = null;
    if(mightBeStateful instanceof StatefulInterceptorFactory) {
      mightBeStateful = ((StatefulInterceptorFactory)mightBeStateful).newInstance();
      if(mightBeStateful instanceof RequiresSession) {
        openSessionEvent = new SetSessionOnRequiresSessionEvent((RequiresSession)mightBeStateful);
      }
    }
    return Pair.of(mightBeStateful, openSessionEvent);
  }

  static Pair<Interceptor, OpenSessionEvent> decorateInterceptor(
      CrossShardRelationshipDetectingInterceptor csrdi,
      Interceptor decorateMe) {
    Pair<Interceptor, OpenSessionEvent> pair = handleStatefulInterceptor(decorateMe);
    Interceptor decorator = new CrossShardRelationshipDetectingInterceptorDecorator(csrdi, pair.first);
    return Pair.of(decorator, pair.second);
  }

  private Object applyGetOperation(
      ShardOperation<Object> shardOp,
      ShardResolutionStrategyData srsd) {
    List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(srsd);
    return
      shardStrategy.getShardAccessStrategy().apply(
          shardIdListToShardList(shardIds),
          shardOp,
          new FirstNonNullResultExitStrategy<Object>(),
          new ExitOperationsQueryCollector());
  }

  private List<Shard> shardIdListToShardList(List<ShardId> shardIds) {
    Set<Shard> shards = Sets.newHashSet();
    for (ShardId shardId : shardIds) {
      shards.add(shardIdsToShards.get(shardId));
    }
    return Lists.newArrayList(shards);
  }

  public List<Shard> getShards() {
    return Collections.unmodifiableList(shards);
  }

  public Object get(final Class clazz, final Serializable id) throws HibernateException {
    ShardOperation<Object> shardOp = new ShardOperation<Object>() {
      public Object execute(Shard shard) {
        return shard.establishSession().get(clazz, id);
      }

      public String getOperationName() {
        return "get(Class class, Serializable id)";
      }
    };
    return applyGetOperation(shardOp, new ShardResolutionStrategyDataImpl(clazz, id));
  }

  public Object get(final Class clazz, final Serializable id, final LockMode lockMode)
      throws HibernateException {
    ShardOperation<Object> shardOp = new ShardOperation<Object>() {
      public Object execute(Shard shard) {
        return shard.establishSession().get(clazz, id, lockMode);
      }

      public String getOperationName() {
        return "get(Class class, Serializable id, LockMode lockMode)";
      }
    };
    // we're not letting people customize shard selection by lockMode
    return applyGetOperation(shardOp, new ShardResolutionStrategyDataImpl(clazz, id));
  }

  public Object get(final String entityName, final Serializable id)
      throws HibernateException {
    ShardOperation<Object> shardOp = new ShardOperation<Object>() {
      public Object execute(Shard shard) {
        return shard.establishSession().get(entityName, id);
      }

      public String getOperationName() {
        return "get(String entityName, Serializable id)";
      }
    };
    return applyGetOperation(shardOp, new ShardResolutionStrategyDataImpl(entityName, id));
  }

  public Object get(final String entityName, final Serializable id, final LockMode lockMode)
      throws HibernateException {
    ShardOperation<Object> shardOp = new ShardOperation<Object>() {
      public Object execute(Shard shard) {
        return shard.establishSession().get(entityName, id, lockMode);
      }

      public String getOperationName() {
        return "get(String entityName, Serializable id, LockMode lockMode)";
      }

    };
    // we're not letting people customize shard selection by lockMode
    return applyGetOperation(shardOp, new ShardResolutionStrategyDataImpl(entityName, id));
  }

  private Session getSomeSession() {
    for (Shard shard : shards) {
      if (shard.getSession() != null) {
        return shard.getSession();
      }
    }
    return null;
  }

  public EntityMode getEntityMode() {
    // assume they all have the same EntityMode
    Session someSession = getSomeSession();
    if (someSession == null) {
      someSession = shards.get(0).establishSession();
    }
    return someSession.getEntityMode();
  }

  /**
   * Unsupported.  This is a scope decision, not a technical decision.
   */
  public Session getSession(EntityMode entityMode) {
    throw new UnsupportedOperationException();
  }

  public void flush() throws HibernateException {
    for (Shard shard : shards) {
      // unopened sessions won't have anything to flush
      if (shard.getSession() != null) {
        shard.getSession().flush();
      }
    }
  }

  public void setFlushMode(FlushMode flushMode) {
    SetFlushModeOpenSessionEvent event = new SetFlushModeOpenSessionEvent(
        flushMode);
    for (Shard shard : shards) {
      if (shard.getSession() != null) {
        shard.getSession().setFlushMode(flushMode);
      } else {
        shard.addOpenSessionEvent(event);
      }
    }
  }

  public FlushMode getFlushMode() {
    // all shards must have the same flush mode
    Session someSession = getSomeSession();
    if (someSession == null) {
      someSession = shards.get(0).establishSession();
    }
    return someSession.getFlushMode();
  }

  public void setCacheMode(CacheMode cacheMode) {
    SetCacheModeOpenSessionEvent event = new SetCacheModeOpenSessionEvent(
        cacheMode);
    for (Shard shard : shards) {
      if (shard.getSession() != null) {
        shard.getSession().setCacheMode(cacheMode);
      } else {
        shard.addOpenSessionEvent(event);
      }
    }
  }

  public CacheMode getCacheMode() {
    // all shards must have the same cache mode
    Session someSession = getSomeSession();
    if (someSession == null) {
      someSession = shards.get(0).establishSession();
    }
    return someSession.getCacheMode();
  }

  public ShardedSessionFactoryImplementor getSessionFactory() {
    return shardedSessionFactory;
  }

  /**
   * @deprecated
   */
  public Connection connection() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Connection close() throws HibernateException {
    List<Throwable> thrown = null;
    for (Shard shard : shards) {
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
      Throwable first = thrown.get(0);
      if (HibernateException.class.isAssignableFrom(first.getClass())) {
        throw (HibernateException)first;
      }
      throw new HibernateException(first);
    }
    closed = true;
    // TODO(maxr) what should I return here?
    return null;
  }

  public void cancelQuery() throws HibernateException {
    // cancel across all shards
    for (Shard shard : shards) {
      if (shard.getSession() != null) {
        shard.getSession().cancelQuery();
      }
    }
  }

  public boolean isOpen() {
    // one open session means the sharded session is open
    for (Shard shard : shards) {
      if (shard.getSession() != null && shard.getSession().isOpen()) {
        return true;
      }
    }
    return false;
  }

  public boolean isConnected() {
    // one connected shard means the session as a whole is connected
    for (Shard shard : shards) {
      if (shard.getSession() != null) {
        if (shard.getSession().isConnected()) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean isDirty() throws HibernateException {
    // one dirty shard is all it takes
    for (Shard shard : shards) {
      if (shard.getSession() != null) {
        if (shard.getSession().isDirty()) {
          return true;
        }
      }
    }
    return false;
  }

  public Serializable getIdentifier(Object object) throws HibernateException {

    for (Shard shard : shards) {
      if (shard.getSession() != null) {
        try {
          return shard.getSession().getIdentifier(object);
        } catch(TransientObjectException e) {
          // Object is transient or is not associated with this session.
        }
      }
    }
    throw new TransientObjectException("Instance is transient or associated with a defferent Session");
  }

  public boolean contains(Object object) {
    for (Shard shard : shards) {
      if (shard.getSession() != null) {
        if (shard.getSession().contains(object)) {
          return true;
        }
      }
    }
    return false;
  }

  public void evict(Object object) throws HibernateException {
    for (Shard shard : shards) {
      if (shard.getSession() != null) {
        shard.getSession().evict(object);
      }
    }
  }

  public Object load(Class clazz, Serializable id, LockMode lockMode)
      throws HibernateException {
    List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(new
                                 ShardResolutionStrategyDataImpl(clazz, id));
    if (shardIds.size() == 1) {
      return shardIdsToShards.get(shardIds.get(0)).establishSession().load(clazz, id, lockMode);
    } else {
      Object result = get(clazz, id, lockMode);
      if (result == null) {
        shardedSessionFactory.getEntityNotFoundDelegate().handleEntityNotFound(clazz.getName(), id);
      }
      return result;
    }
  }

  public Object load(String entityName, Serializable id, LockMode lockMode)
      throws HibernateException {
    List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(new
                                 ShardResolutionStrategyDataImpl(entityName, id));
    if (shardIds.size() == 1) {
      return shardIdsToShards.get(shardIds.get(0)).establishSession().load(entityName, id, lockMode);
    } else {
      Object result = get(entityName, id, lockMode);
      if (result == null) {
        shardedSessionFactory.getEntityNotFoundDelegate().handleEntityNotFound(entityName, id);
      }
      return result;
    }
  }

  public Object load(Class clazz, Serializable id)
      throws HibernateException {
    List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(new
                                 ShardResolutionStrategyDataImpl(clazz, id));
    if (shardIds.size() == 1) {
      return shardIdsToShards.get(shardIds.get(0)).establishSession().load(clazz, id);
    } else {
      Object result = get(clazz, id);
      if (result == null) {
        shardedSessionFactory.getEntityNotFoundDelegate().handleEntityNotFound(clazz.getName(), id);
      }
      return result;
    }
  }

  public Object load(String entityName, Serializable id)
      throws HibernateException {
    List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(new
                                 ShardResolutionStrategyDataImpl(entityName, id));
    if (shardIds.size() == 1) {
      return shardIdsToShards.get(shardIds.get(0)).establishSession().load(entityName, id);
    } else {
      Object result = get(entityName, id);
      if (result == null) {
        shardedSessionFactory.getEntityNotFoundDelegate().handleEntityNotFound(entityName, id);
      }
      return result;
    }
  }

  public void load(Object object, Serializable id) throws HibernateException {
    List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(new
                                 ShardResolutionStrategyDataImpl(object.getClass(), id));
    if (shardIds.size() == 1) {
      shardIdsToShards.get(shardIds.get(0)).establishSession().load(object, id);
    } else {
      Object result = get(object.getClass(), id);
      if (result == null) {
        shardedSessionFactory.getEntityNotFoundDelegate().handleEntityNotFound(object.getClass().getName(), id);
      } else {
        Shard objectShard = getShardForObject(result, shardIdListToShardList(shardIds));
        evict(result);
        objectShard.establishSession().load(object, id);
      }
    }
  }

  public void replicate(Object object, ReplicationMode replicationMode)
      throws HibernateException {
    replicate(null, object, replicationMode);
  }

  public void replicate(String entityName, Object object,
      ReplicationMode replicationMode) throws HibernateException {
    Serializable id = extractId(object);
    List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(new
                                 ShardResolutionStrategyDataImpl(object.getClass(), id));
    if (shardIds.size() == 1) {
      setCurrentSubgraphShardId(shardIds.get(0));
      shardIdsToShards.get(shardIds.get(0)).establishSession().replicate(entityName, object, replicationMode);
    } else {
      Object result = null;
      if (id != null) result = get(object.getClass(), id);
      if (result == null) {  // non-persisted object
        ShardId shardId = selectShardIdForNewObject(object);
        setCurrentSubgraphShardId(shardId);
        shardIdsToShards.get(shardId).establishSession().replicate(entityName, object, replicationMode);
      } else {
        Shard objectShard = getShardForObject(result, shardIdListToShardList(shardIds));
        evict(result);
        objectShard.establishSession().replicate(entityName, object, replicationMode);
      }
    }
  }

  public Serializable save(String entityName, Object object) throws HibernateException {
    // TODO(tomislav): what if we have detached instance?
    ShardId shardId = getShardIdForObject(object);
    if(shardId == null) {
      shardId = selectShardIdForNewObject(object);
    }
    Preconditions.checkNotNull(shardId);
    setCurrentSubgraphShardId(shardId);
    log.debug(String.format("Saving object of type %s to shard %s", object.getClass(), shardId));
    return shardIdsToShards.get(shardId).establishSession().save(entityName, object);
  }

  ShardId selectShardIdForNewObject(Object obj) {
    if(lockedShardId != null) {
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
    if(shardId == null) {
      checkForUnsupportedTopLevelSave(obj.getClass());
      shardId = shardStrategy.getShardSelectionStrategy().selectShardIdForNewObject(obj);
    }
    // lock has been requested but shard has not yet been selected - lock it in
    if(lockedShard) {
      lockedShardId = shardId;
    }
    log.debug(String.format("Selected shard %d for object of type %s", shardId.getId(), obj.getClass().getName()));
    return shardId;
  }

  /*
   * We already know that we don't have a shardId locked in for this session,
   * and we already know that this object can't grab its session from some
   * other object (we looked).  If this class is in the set of classes
   * that don't support top-level saves, it's an error.
   * This is to prevent clients from accidentally splitting their object graphs
   * across multiple shards.
   */
  private void checkForUnsupportedTopLevelSave(Class<?> clazz) {
    if(classesWithoutTopLevelSaveSupport.contains(clazz)) {
      final String msg = String.format(
          "Attempt to save object of type %s as a top-level object.",
          clazz.getName());
      log.error(msg);
      throw new HibernateException(msg);
    }
  }

  /**
   * TODO(maxr) I can see this method benefitting from a cache that lets us quickly
   * see which properties we might need to look at.
   */
  ShardId getShardIdOfRelatedObject(Object obj) {
    ClassMetadata cmd = getClassMetadata(obj.getClass());
    Type[] types = cmd.getPropertyTypes();
    // TODO(maxr) fix hard-coded entity mode
    Object[] values = cmd.getPropertyValues(obj, EntityMode.POJO);
    ShardId shardId = null;
    List<Collection<Object>> collections = null;
    for(Pair<Type, Object> pair : CrossShardRelationshipDetectingInterceptor.buildListOfAssociations(types, values)) {
      if(pair.getFirst().isCollectionType()) {
        /**
         * collection types are more expensive to evaluate (might involve
         * lazy-loading the contents of the collection from the db), so
         * let's hold off until the end on the chance that we can fail
         * quickly.
         */
        if(collections == null) {
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
        if(shardId != null && !checkAllAssociatedObjectsForDifferentShards) {
          return shardId;
        }
      }
    }
    if(collections != null) {
      for(Object collEntry : Iterables.concat(collections)) {
        shardId = checkForConflictingShardId(shardId, obj.getClass(), collEntry);
        if(shardId != null && !checkAllAssociatedObjectsForDifferentShards) {
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
    if(localShardId != null) {
      if(existingShardId == null) {
        existingShardId = localShardId;
      } else if(!localShardId.equals(existingShardId)) {
        final String msg = String.format(
            "Object of type %s is on shard %d but an associated object of type %s is on shard %d.",
            newObjectClass.getName(),
            existingShardId.getId(),
            associatedObject.getClass().getName(),
            localShardId.getId());
        log.error(msg);
        throw new CrossShardAssociationException(msg);
      }
    }
    return existingShardId;
  }

  ClassMetadata getClassMetadata(Class<?> clazz) {
    return getSessionFactory().getClassMetadata(clazz);
  }

  public Serializable save(Object object)
      throws HibernateException {
    return save(null, object);
  }

  public void saveOrUpdate(Object object) throws HibernateException {
    applySaveOrUpdateOperation(SAVE_OR_UPDATE_SIMPLE, object);
  }

  public void saveOrUpdate(final String entityName, Object object)
      throws HibernateException {
    SaveOrUpdateOperation op = new SaveOrUpdateOperation() {
      public void saveOrUpdate(Shard shard, Object object) {
        shard.establishSession().saveOrUpdate(entityName, object);
      }

      public void merge(Shard shard, Object object) {
        shard.establishSession().merge(entityName, object);
      }
    };
    applySaveOrUpdateOperation(op, object);
  }

  void applySaveOrUpdateOperation(SaveOrUpdateOperation op, Object object) {
    ShardId shardId = getShardIdForObject(object);
    if(shardId != null) {
      // attached object
      op.saveOrUpdate(shardIdsToShards.get(shardId), object);
      return;
    }
    List<Shard> potentialShards = determineShardsObjectViaResolutionStrategy(object);
    if(potentialShards.size() == 1) {
      op.saveOrUpdate(potentialShards.get(0), object);
      return;
    }

    /**
     * Too bad, we've got a detached object that could be on more than 1 shard.
     * The only safe way to handle this is to try and lookup the object, and if
     * it exists, do a merge, and if it doesn't, do a save.
     */
    Serializable id = extractId(object);
    if(id != null) {
      Object persistent = get(object.getClass(), id);
      if(persistent != null) {
        shardId = getShardIdForObject(persistent);
      }
    }
    if(shardId != null) {
      op.merge(shardIdsToShards.get(shardId), object);
    } else {
      save(object);
    }
  }

  Serializable extractId(Object object) {
    ClassMetadata cmd = shardedSessionFactory.getClassMetadata(object.getClass());
    // I'm just guessing about the EntityMode
    return cmd.getIdentifier(object, EntityMode.POJO);
  }

  private interface UpdateOperation {
    void update(Shard shard, Object object);
    void merge(Shard shard, Object object);
  }

  private static final UpdateOperation SIMPLE_UPDATE_OPERATION =
      new UpdateOperation() {
        public void update(Shard shard, Object object) {
          shard.establishSession().update(object);
        }

        public void merge(Shard shard, Object object) {
          shard.establishSession().merge(object);
        }
      };

  private void applyUpdateOperation(UpdateOperation op, Object object) {
    ShardId shardId = getShardIdForObject(object);
    if(shardId != null) {
      // attached object
      op.update(shardIdsToShards.get(shardId), object);
      return;
    }
    List<Shard> potentialShards = determineShardsObjectViaResolutionStrategy(object);
    if(potentialShards.size() == 1) {
      op.update(potentialShards.get(0), object);
      return;
    }
    /**
     * Too bad, we've got a detached object that could be on more than 1 shard.
     * The only safe way to perform the update is to load the object and then
     * do a merge.
     */
    Serializable id = extractId(object);
    if(id != null) {
      Object persistent = get(object.getClass(), extractId(object));
      if(persistent != null) {
        shardId = getShardIdForObject(persistent);
      }
    }
    if(shardId == null) {
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

  public void update(Object object) throws HibernateException {
    applyUpdateOperation(SIMPLE_UPDATE_OPERATION, object);
  }

  public void update(final String entityName, Object object)
      throws HibernateException {
    UpdateOperation op = new UpdateOperation() {
      public void update(Shard shard, Object object) {
        shard.establishSession().update(entityName, object);
      }

      public void merge(Shard shard, Object object) {
        shard.establishSession().merge(entityName, object);
      }
    };
    applyUpdateOperation(op, object);
  }

  List<Shard> determineShardsObjectViaResolutionStrategy(Object object) {
    Serializable id = extractId(object);
    if(id == null) {
      return Collections.emptyList();
    }
    ShardResolutionStrategyData srsd = new ShardResolutionStrategyDataImpl(object.getClass(), id);
    List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(srsd);
    return shardIdListToShardList(shardIds);
  }

  public Object merge(Object object) throws HibernateException {
    return merge(null, object);
  }

  public Object merge(String entityName, Object object)
      throws HibernateException {
    Serializable id = extractId(object);
    List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(new
        ShardResolutionStrategyDataImpl(object.getClass(), id));
    if (shardIds.size() == 1) {
      setCurrentSubgraphShardId(shardIds.get(0));
      return shardIdsToShards.get(shardIds.get(0)).establishSession().merge(entityName, object);
    } else {
      Object result = null;
      if (id != null) result = get(object.getClass(), id);
      if (result == null) {  // non-persisted object
        ShardId shardId = selectShardIdForNewObject(object);
        setCurrentSubgraphShardId(shardId);
        return shardIdsToShards.get(shardId).establishSession().merge(entityName, object);
      } else {
        Shard objectShard = getShardForObject(result, shardIdListToShardList(shardIds));
        return objectShard.establishSession().merge(entityName, object);
      }
    }
  }

  public void persist(Object object) throws HibernateException {
    persist(null, object);
  }

  public void persist(String entityName, Object object)
      throws HibernateException {
    // TODO(tomislav): what if we have detached object?
    ShardId shardId = getShardIdForObject(object);
    if(shardId == null) {
      shardId = selectShardIdForNewObject(object);
    }
    Preconditions.checkNotNull(shardId);
    setCurrentSubgraphShardId(shardId);
    log.debug(String.format("Persisting object of type %s to shard %s", object.getClass(), shardId));
    shardIdsToShards.get(shardId).establishSession().persist(entityName, object);
  }

  private interface DeleteOperation {
    void delete(Shard shard, Object object);
  }

  private void applyDeleteOperation(DeleteOperation op, Object object) {
    ShardId shardId = getShardIdForObject(object);
    if(shardId != null) {
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
    if(potentialShards.size() == 1) {
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

  private static final DeleteOperation SIMPLE_DELETE_OPERATION =
      new DeleteOperation() {

        public void delete(Shard shard, Object object) {
          shard.establishSession().delete(object);
        }
      };

  public void delete(Object object) throws HibernateException {
    applyDeleteOperation(SIMPLE_DELETE_OPERATION, object);
  }

  public void delete(final String entityName, Object object)
      throws HibernateException {
    DeleteOperation op = new DeleteOperation() {
      public void delete(Shard shard, Object object) {
        shard.establishSession().delete(entityName, object);
      }
    };
    applyDeleteOperation(op, object);
  }

  public void lock(final Object object, final LockMode lockMode) throws HibernateException {
    ShardOperation<Void> op = new ShardOperation<Void>() {
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

  public void lock(final String entityName, final Object object, final LockMode lockMode)
      throws HibernateException {
    ShardOperation<Void> op = new ShardOperation<Void>() {
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

  private interface RefreshOperation {
    void refresh(Shard shard, Object object);
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

  public void refresh(final Object object) throws HibernateException {
    RefreshOperation op = new RefreshOperation() {
      public void refresh(Shard shard, Object object) {
        shard.establishSession().refresh(object);
      }
    };
    applyRefreshOperation(op, object);
  }

  public void refresh(final Object object, final LockMode lockMode)
      throws HibernateException {
    RefreshOperation op = new RefreshOperation() {
      public void refresh(Shard shard, Object object) {
        shard.establishSession().refresh(object, lockMode);
      }
    };
    applyRefreshOperation(op, object);
  }

  public LockMode getCurrentLockMode(final Object object) throws HibernateException {
    ShardOperation<LockMode> invoker = new ShardOperation<LockMode>() {
      public LockMode execute(Shard s) {
        return s.establishSession().getCurrentLockMode(object);
      }
      public String getOperationName() {
        return "getCurrentLockmode(Object object)";
      }
    };
    return invokeOnShardWithObject(invoker, object);
  }

  public Transaction beginTransaction() throws HibernateException {
    errorIfClosed();
    Transaction result = getTransaction();
    result.begin();
    return result;
  }

  public Transaction getTransaction() {
    errorIfClosed();
    if (transaction == null) {
      transaction = new ShardedTransactionImpl(this);
    }
    return transaction;
  }

  public Criteria createCriteria(Class persistentClass) {
    return new ShardedCriteriaImpl(
        new CriteriaId(nextCriteriaId++),
        shards,
        new CriteriaFactoryImpl(persistentClass),
        shardStrategy.getShardAccessStrategy());
  }

  public Criteria createCriteria(Class persistentClass, String alias) {
    return new ShardedCriteriaImpl(
        new CriteriaId(nextCriteriaId++),
        shards,
        new CriteriaFactoryImpl(persistentClass, alias),
        shardStrategy.getShardAccessStrategy());
  }

  public Criteria createCriteria(String entityName) {
    return new ShardedCriteriaImpl(
        new CriteriaId(nextCriteriaId++),
        shards,
        new CriteriaFactoryImpl(entityName),
        shardStrategy.getShardAccessStrategy());
  }

  public Criteria createCriteria(String entityName, String alias) {
    return new ShardedCriteriaImpl(
        new CriteriaId(nextCriteriaId++),
        shards,
        new CriteriaFactoryImpl(entityName, alias),
        shardStrategy.getShardAccessStrategy());
  }

  public Query createQuery(String queryString) throws HibernateException {
    return new ShardedQueryImpl(new QueryId(nextQueryId++),
            shards,
            new AdHocQueryFactoryImpl(queryString),
            shardStrategy.getShardAccessStrategy());
  }

  /**
   * Unsupported.  This is a scope decision, not a technical decision.
   */
  public SQLQuery createSQLQuery(String queryString) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * The {@link org.hibernate.impl.SessionImpl#createFilter(Object, String)} implementation
   * requires that the collection that is passed in is a persistent collection.
   * Since we don't support cross-shard relationships, if we receive a persistent
   * collection that collection is guaranteed to be associated with a single
   * shard.  If we can figure out which shard the collection is associated with
   * we can just delegate this operation to that shard.
   */
  public Query createFilter(Object collection, String queryString)
      throws HibernateException {
    Shard shard = getShardForCollection(collection, shards);
    Session session;
    if(shard == null) {
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

  public Query getNamedQuery(String queryName) throws HibernateException {

    return new ShardedQueryImpl(new QueryId(nextQueryId++),
            shards,
            new NamedQueryFactoryImpl(queryName),
            shardStrategy.getShardAccessStrategy());
  }

  public void clear() {
    for (Shard shard : shards) {
      if (shard.getSession() != null) {
        shard.getSession().clear();
      }
    }
  }

  public String getEntityName(final Object object) throws HibernateException {
    ShardOperation<String> invoker = new ShardOperation<String>() {
      public String execute(Shard s) {
        return s.establishSession().getEntityName(object);
      }
      public String getOperationName() {
        return "getEntityName(Object object)";
      }
    };
    return invokeOnShardWithObject(invoker, object);
  }

  public Filter enableFilter(String filterName) {
    EnableFilterOpenSessionEvent event = new EnableFilterOpenSessionEvent(
        filterName);
    for (Shard shard : shards) {
      if (shard.getSession() != null) {
        shard.getSession().enableFilter(filterName);
      } else {
        shard.addOpenSessionEvent(event);
      }
    }
    // TODO(maxr) what do we return here?  A sharded filter?
    return null;
  }

  public Filter getEnabledFilter(String filterName) {
    // all session have same filters
    for (Shard shard : shards) {
      if (shard.getSession() != null) {
        Filter filter = shard.getSession().getEnabledFilter(filterName);
        if (filter != null) {
          return filter;
        }
      }
    }
    // TODO(maxr) what do we return here?
    return null;
  }

  public void disableFilter(String filterName) {
    DisableFilterOpenSessionEvent event = new DisableFilterOpenSessionEvent(
        filterName);
    for (Shard shard : shards) {
      if (shard.getSession() != null) {
        shard.getSession().disableFilter(filterName);
      } else {
        shard.addOpenSessionEvent(event);
      }
    }
  }

  public SessionStatistics getStatistics() {
    return new ShardedSessionStatistics(this);
  }

  public void setReadOnly(Object entity, boolean readOnly) {
    SetReadOnlyOpenSessionEvent event = new SetReadOnlyOpenSessionEvent(entity,
        readOnly);
    for (Shard shard : shards) {
      if (shard.getSession() != null) {
        shard.getSession().setReadOnly(entity, readOnly);
      } else {
        shard.addOpenSessionEvent(event);
      }
    }
  }

  public Connection disconnect() throws HibernateException {
    for (Shard s : getShards()) {
      s.getSession().disconnect();
    }
    // we do not allow application-supplied connections, so we can always return
    // null
    return null;
  }

  /**
   * @deprecated
   */
  public void reconnect() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported.  This is a technical decision.
   */
  public void reconnect(Connection connection) throws HibernateException {
    throw new UnsupportedOperationException(
        "Cannot reconnect a sharded session");
  }

  /**
   * All methods below fulfill the org.hibernate.classic.Session interface.
   * These methods are all deprecated, and since we don't really have any
   * legacy Hibernate code at Google so we're simply not going to support them.
   */

  /**
   * @deprecated
   */
  public Object saveOrUpdateCopy(Object object) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public Object saveOrUpdateCopy(Object object, Serializable id)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public Object saveOrUpdateCopy(String entityName, Object object)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public Object saveOrUpdateCopy(String entityName, Object object,
      Serializable id) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public List find(String query) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public List find(String query, Object value, Type type)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public List find(String query, Object[] values, Type[] types)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public Iterator iterate(String query) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public Iterator iterate(String query, Object value, Type type)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public Iterator iterate(String query, Object[] values, Type[] types)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public Collection filter(Object collection, String filter)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public Collection filter(Object collection, String filter, Object value,
      Type type) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public Collection filter(Object collection, String filter, Object[] values,
      Type[] types) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public int delete(String query) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public int delete(String query, Object value, Type type)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public int delete(String query, Object[] values, Type[] types)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public Query createSQLQuery(String sql, String returnAlias,
      Class returnClass) {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public Query createSQLQuery(String sql, String[] returnAliases,
      Class[] returnClasses) {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public void save(Object object, Serializable id) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public void save(String entityName, Object object, Serializable id)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public void update(Object object, Serializable id) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated
   */
  public void update(String entityName, Object object, Serializable id)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  void errorIfClosed() {
    if (closed) {
      throw new SessionException( "Session is closed!" );
    }
  }

  interface SaveOrUpdateOperation {
    void saveOrUpdate(Shard shard, Object object);
    void merge(Shard shard, Object object);
  }

  private static final SaveOrUpdateOperation SAVE_OR_UPDATE_SIMPLE = new SaveOrUpdateOperation() {
    public void saveOrUpdate(Shard shard, Object object) {
      shard.establishSession().saveOrUpdate(object);
    }

    public void merge(Shard shard, Object object) {
      shard.establishSession().merge(object);
    }
  };

  private Shard getShardForObject(Object obj, List<Shard> shardsToConsider) {
    for(Shard shard : shardsToConsider) {
      if(shard.getSession() != null && shard.getSession().contains(obj)) {
        return shard;
      }
    }
    return null;
  }

  private Session getSessionForObject(Object obj, List<Shard> shardsToConsider) {
    Shard shard = getShardForObject(obj, shardsToConsider);
    if(shard == null) {
      return null;
    }
    return shard.getSession();
  }

  public Session getSessionForObject(Object obj) {
    return getSessionForObject(obj, shards);
  }

  public ShardId getShardIdForObject(Object obj, List<Shard> shardsToConsider) {
    // TODO(maxr) optimize this by keeping an identity map of objects to shardId
    Shard shard = getShardForObject(obj, shardsToConsider);
    if(shard == null) {
      return null;
    } else if (shard.getShardIds().size() == 1) {
      return shard.getShardIds().iterator().next();
    } else {
      String className;
      if (obj instanceof HibernateProxy) {
        className = ((HibernateProxy)obj).getHibernateLazyInitializer().getPersistentClass().getName();
      } else {
        className = obj.getClass().getName();
      }
      IdentifierGenerator idGenerator = shard.getSessionFactoryImplementor().getIdentifierGenerator(className);
      if (idGenerator instanceof ShardEncodingIdentifierGenerator) {
        return ((ShardEncodingIdentifierGenerator)idGenerator).extractShardId(getIdentifier(obj));
      } else {
        // TODO(tomislav): also use shard resolution strategy if it returns only 1 shard; throw this error in config instead of here
        throw new HibernateException("Can not use virtual sharding with non-shard resolving id gen");
      }
    }
  }

  public ShardId getShardIdForObject(Object obj) {
    return getShardIdForObject(obj, shards);
  }

  private Shard getShardForCollection(Object coll, List<Shard> shardsToConsider) {
    for(Shard shard : shardsToConsider) {
      if(shard.getSession() != null) {
        SessionImplementor si = ((SessionImplementor)shard.getSession());
        if(si.getPersistenceContext().getCollectionEntryOrNull(coll) != null) {
          return shard;
        }
      }
    }
    return null;
  }

  List<ShardId> selectShardIdsFromShardResolutionStrategyData(ShardResolutionStrategyData srsd) {
    IdentifierGenerator idGenerator = shardedSessionFactory.getIdentifierGenerator(srsd.getEntityName());
    if ((idGenerator instanceof ShardEncodingIdentifierGenerator) &&
        (srsd.getId() != null)) {
      return Collections.singletonList(((ShardEncodingIdentifierGenerator)idGenerator).extractShardId(srsd.getId()));
    }
    return shardStrategy.getShardResolutionStrategy().selectShardIdsFromShardResolutionStrategyData(srsd);
  }

  public void lockShard() {
    lockedShard = true;
  }

  public boolean getCheckAllAssociatedObjectsForDifferentShards() {
    return checkAllAssociatedObjectsForDifferentShards;
  }

  @Override
  protected void finalize() throws Throwable {
    try {
      if(!closed) {
        log.warn("ShardedSessionImpl is being garbage collected but it was never properly closed.");
        try {
          close();
        } catch (Exception e) {
          log.warn("Caught exception trying to close.", e);
        }
      }
    } finally {
      super.finalize();
    }
  }

  public static ShardId getCurrentSubgraphShardId() {
    return currentSubgraphShardId.get();
  }

  public static void setCurrentSubgraphShardId(ShardId shardId) {
    currentSubgraphShardId.set(shardId);
  }

  /**
   * Helper method we can use when we need to find the Shard with which a
   * specified object is associated and invoke the method on that Shard.
   * If the object isn't associated with a Session we just invoke it on a
   * random Session with the expectation that this will cause an error.
   */
  <T> T invokeOnShardWithObject(ShardOperation<T> so, Object object) throws HibernateException {
    ShardId shardId = getShardIdForObject(object);
    Shard shardToUse;
    if (shardId == null) {
      // just ask this question of a random shard so we get the proper error
      shardToUse = shards.get(0);
    } else {
      shardToUse = shardIdsToShards.get(shardId);
    }
    return so.execute(shardToUse);
  }



}

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
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.shards.CrossShardAssociationException;
import org.hibernate.shards.ShardId;
import org.hibernate.shards.util.Iterables;
import org.hibernate.shards.util.Lists;
import org.hibernate.shards.util.Pair;
import org.hibernate.shards.util.Preconditions;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Interceptor that looks at all associated objects and verifies that those
 * objects are on the proper shard.  There is a definite performance impact
 * associated with this interceptor since we'll end up resolving all the lazy
 * associations.
 *
 * @author maxr@google.com (Max Ross)
 */
class CrossShardRelationshipDetectingInterceptor extends EmptyInterceptor {

  private final ShardIdResolver shardIdResolver;

  private final Log log = LogFactory.getLog(getClass());

  public CrossShardRelationshipDetectingInterceptor(ShardIdResolver shardIdResolver) {
    Preconditions.checkNotNull(shardIdResolver);
    this.shardIdResolver = shardIdResolver;
  }

  @Override
  public boolean onFlushDirty(Object entity, Serializable id,
      Object[] currentState, Object[] previousState, String[] propertyNames,
      Type[] types) throws CallbackException {
    ShardId expectedShardId = getAndRefreshExpectedShardId(entity);
    Preconditions.checkNotNull(expectedShardId);

    List<Collection<Object>> collections = null;
    for(Pair<Type, Object> pair : buildListOfAssociations(types, currentState)) {
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
        Collection<Object> objColl = (Collection<Object>) pair.getSecond();
        collections.add(objColl);
      } else {
        checkForConflictingShardId(entity.getClass().getName(), expectedShardId, pair.getSecond());
      }
    }
    if(collections != null) {
      checkIterable(entity.getClass().getName(), expectedShardId, Iterables.concat(collections));
    }
    return false;
  }

  static List<Pair<Type, Object>> buildListOfAssociations(Type[] types, Object[] currentState) {
    // we assume types and current state are the same length
    Preconditions.checkState(types.length == currentState.length);
    List<Pair<Type, Object>> associationList = Lists.newArrayList();
    for(int i = 0; i < types.length; i++) {
      if(types[i] != null &&
          currentState[i] != null &&
          types[i].isAssociationType()) {
        associationList.add(Pair.of(types[i], currentState[i]));
      }
    }
    return associationList;
}

  void checkIterable(String classOfUpdatedObject, ShardId expectedShardId, Iterable<Object> iterable) {
    for(Object obj : iterable) {
      checkForConflictingShardId(classOfUpdatedObject, expectedShardId, obj);
    }
  }

  void checkForConflictingShardId(String classOfUpdatedObject, ShardId expectedShardId, Object associatedObject) {
    ShardId localShardId;
    /*
     * Here's something you wish you didn't need to know: If the associated
     * object is an unitialized proxy and the object is not on the same
     * shard as the shard with which the interceptor is associated, attempting
     * to lookup the shard for the object will yield an ObjectNotFoundException
     * that Hibernate will swallow, and getShardIdForObject will return null and
     * the association will be let through.
     * In order to avoid this, we check to see if the associated object is
     * a proxy, and if it is we force it to initialize.
     * If the associated object is a pojo or a proxy that has already been
     * initialized, the call to getShardIdForObject will succeed.
     */
    if(associatedObject instanceof HibernateProxy) {
      HibernateProxy hp = (HibernateProxy) associatedObject;
      try {
        hp.getHibernateLazyInitializer().initialize();
      } catch(ObjectNotFoundException e) {
        final String msg = String.format(
            "Object of type %s is on shard %d but an associated object of type %s is on different shard.",
            classOfUpdatedObject,
            expectedShardId.getId(),
            hp.getHibernateLazyInitializer().getPersistentClass().getName());
        log.error(msg);
        throw new CrossShardAssociationException(msg);
      }
    }
    localShardId = shardIdResolver.getShardIdForObject(associatedObject);
    if(localShardId != null) {
      if(!localShardId.equals(expectedShardId)) {
        final String msg = String.format(
            "Object of type %s is on shard %d but an associated object of type %s is on shard %d.",
            classOfUpdatedObject,
            expectedShardId.getId(),
            associatedObject.getClass().getName(),
            localShardId.getId());
        log.error(msg);
        throw new CrossShardAssociationException(msg);
      }
    }
  }

  @Override
  public void onCollectionUpdate(Object collection, Serializable key)
      throws CallbackException {
    ShardId expectedShardId = getAndRefreshExpectedShardId(((PersistentCollection)collection).getOwner());
    Preconditions.checkNotNull(expectedShardId);
    @SuppressWarnings("unchecked")
    Iterable<Object> iterable = (Iterable<Object>) collection;
    checkIterable("<Unknown>", expectedShardId, iterable);
  }

  private ShardId getAndRefreshExpectedShardId(Object object) {
    ShardId expectedShardId = shardIdResolver.getShardIdForObject(object);
    if (expectedShardId == null) {
      expectedShardId = ShardedSessionImpl.getCurrentSubgraphShardId();
    } else {
      ShardedSessionImpl.setCurrentSubgraphShardId(expectedShardId);
    }
    return expectedShardId;
  }
}

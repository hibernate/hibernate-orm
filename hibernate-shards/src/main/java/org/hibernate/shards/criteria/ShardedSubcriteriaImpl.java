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

package org.hibernate.shards.criteria;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.impl.CriteriaImpl;
import org.hibernate.shards.Shard;
import org.hibernate.shards.util.Lists;
import org.hibernate.shards.util.Maps;
import org.hibernate.shards.util.Preconditions;
import org.hibernate.transform.ResultTransformer;

import java.util.List;
import java.util.Map;

/**
 * Concrete implementation of the {@link ShardedSubcriteria} interface.
 * You'll notice that this class does not extend {@link ShardedCriteria}.
 * Why? Because {@link CriteriaImpl.Subcriteria} doesn't extend {@link Criteria}.  We
 * don't actually need the entire {@link Criteria} interface.
 *
 * @author maxr@google.com (Max Ross)
 */
class ShardedSubcriteriaImpl implements ShardedSubcriteria {

  // all shards that we're aware of
  final List<Shard> shards;

  // our parent. As with CriteriaImpl, we pass-through certain operations
  // to our parent
  final ShardedCriteria parent;

  // maps shards to actual Criteria objects
  private final Map<Shard, Criteria> shardToCriteriaMap = Maps.newHashMap();

  // maps shards to lists of criteria events that need to be applied
  // when the actual Criteria objects are established
  private final Map<Shard, List<CriteriaEvent>> shardToEventListMap = Maps.newHashMap();

  /**
   * Construct a ShardedSubcriteriaImpl
   *
   * @param shards the shards that we're aware of
   * @param parent our parent
   */
  public ShardedSubcriteriaImpl(List<Shard> shards, ShardedCriteria parent) {
    Preconditions.checkNotNull(shards);
    Preconditions.checkNotNull(parent);
    Preconditions.checkArgument(!shards.isEmpty());
    this.shards = shards;
    this.parent = parent;
    // let's set up our maps
    for(Shard shard : shards) {
      shardToCriteriaMap.put(shard, null);
      shardToEventListMap.put(shard, Lists.<CriteriaEvent>newArrayList());
    }
  }

  /**
   * @return Returns an actual Criteria object, or null if none have been allocated.
   */
  private /*@Nullable*/ Criteria getSomeSubcriteria() {
    for (Criteria crit : shardToCriteriaMap.values()) {
      if (crit != null) {
        return crit;
      }
    }
    return null;
  }

  /**
   * @return Returns an actual Criteria object.  If no actual Criteria object
   * has been allocated, allocate one and return it.
   */
  private Criteria getOrEstablishSomeSubcriteria() {
    Criteria crit = getSomeSubcriteria();
    if(crit == null) {
      Shard shard = shards.get(0);
      // this should trigger the creation of all subcriteria for the parent
      shard.establishCriteria(parent);
    }
    return getSomeSubcriteria();
  }

  public String getAlias() {
    return getOrEstablishSomeSubcriteria().getAlias();
  }

  public Criteria setProjection(Projection projection) {
    CriteriaEvent event = new SetProjectionEvent(projection);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).setProjection(projection);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  public Criteria add(Criterion criterion) {
    CriteriaEvent event = new AddCriterionEvent(criterion);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).add(criterion);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  public Criteria addOrder(Order order) {
    CriteriaEvent event = new AddOrderEvent(order);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).addOrder(order);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  public Criteria setFetchMode(String associationPath, FetchMode mode)
      throws HibernateException {
    CriteriaEvent event = new SetFetchModeEvent(associationPath, mode);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).setFetchMode(associationPath, mode);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  public Criteria setLockMode(LockMode lockMode) {
    CriteriaEvent event = new SetLockModeEvent(lockMode);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).setLockMode(lockMode);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  public Criteria setLockMode(String alias, LockMode lockMode) {
    CriteriaEvent event = new SetLockModeEvent(lockMode, alias);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).setLockMode(alias, lockMode);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  public Criteria createAlias(String associationPath, String alias)
      throws HibernateException {
    CriteriaEvent event = new CreateAliasEvent(associationPath, alias);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).createAlias(associationPath, alias);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  public Criteria createAlias(String associationPath, String alias,
      int joinType) throws HibernateException {
    CriteriaEvent event = new CreateAliasEvent(associationPath, alias, joinType);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).createAlias(associationPath, alias, joinType);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  public Criteria setResultTransformer(ResultTransformer resultTransformer) {
    CriteriaEvent event = new SetResultTransformerEvent(resultTransformer);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).setResultTransformer(resultTransformer);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  /**
   * TODO(maxr)
   * This clearly isn't what people want.  We should be building an
   * exit strategy that returns once we've accumulated maxResults
   * across _all_ shards, not each shard.
   */
  public Criteria setMaxResults(int maxResults) {
    CriteriaEvent event = new SetMaxResultsEvent(maxResults);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).setMaxResults(maxResults);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  public Criteria setFirstResult(int firstResult) {
    CriteriaEvent event = new SetFirstResultEvent(firstResult);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).setFirstResult(firstResult);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  public Criteria setFetchSize(int fetchSize) {
    CriteriaEvent event = new SetFetchSizeEvent(fetchSize);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).setFetchSize(fetchSize);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  public Criteria setTimeout(int timeout) {
    CriteriaEvent event = new SetTimeoutEvent(timeout);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).setTimeout(timeout);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  public Criteria setCacheable(boolean cacheable) {
    CriteriaEvent event = new SetCacheableEvent(cacheable);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).setCacheable(cacheable);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  public Criteria setCacheRegion(String cacheRegion) {
    CriteriaEvent event = new SetCacheRegionEvent(cacheRegion);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).setCacheRegion(cacheRegion);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  public Criteria setComment(String comment) {
    CriteriaEvent event = new SetCommentEvent(comment);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).setComment(comment);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  public Criteria setFlushMode(FlushMode flushMode) {
    CriteriaEvent event = new SetFlushModeEvent(flushMode);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).setFlushMode(flushMode);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  public Criteria setCacheMode(CacheMode cacheMode) {
    CriteriaEvent event = new SetCacheModeEvent(cacheMode);
    for (Shard shard : shards) {
      if (shardToCriteriaMap.get(shard) != null) {
        shardToCriteriaMap.get(shard).setCacheMode(cacheMode);
      } else {
        shardToEventListMap.get(shard).add(event);
      }
    }
    return this;
  }

  public List list() throws HibernateException {
    // pass through to the parent
    return getParentCriteria().list();
  }

  public ScrollableResults scroll() throws HibernateException {
    // pass through to the parent
    return getParentCriteria().scroll();
  }

  public ScrollableResults scroll(ScrollMode scrollMode)
      throws HibernateException {
    // pass through to the parent
    return getParentCriteria().scroll(scrollMode);
  }

  public Object uniqueResult() throws HibernateException {
    // pass through to the parent
    return getParentCriteria().uniqueResult();
  }

  private ShardedSubcriteriaImpl createSubcriteria(SubcriteriaFactory factory) {
    // first build our sharded subcrit
    ShardedSubcriteriaImpl subcrit = new ShardedSubcriteriaImpl(shards, parent);
    for (Shard shard : shards) {
      // see if we already have a concreate Criteria object for each shard
      if (shardToCriteriaMap.get(shard) != null) {
        // we already have a concreate Criteria for this shard, so create
        // a subcrit for it using the provided factory
        factory.createSubcriteria(this, shardToEventListMap.get(shard));
      } else {
        // we do not yet have a concrete Criteria object for this shard
        // so register an event that will create a proper subcrit when we do
        CreateSubcriteriaEvent event = new CreateSubcriteriaEvent(factory, subcrit.getSubcriteriaRegistrar(shard));
        shardToEventListMap.get(shard).add(event);
      }
    }
    return subcrit;
  }

  SubcriteriaRegistrar getSubcriteriaRegistrar(final Shard shard) {
    return new SubcriteriaRegistrar() {

      public void establishSubcriteria(Criteria parentCriteria, SubcriteriaFactory subcriteriaFactory) {
        List<CriteriaEvent> criteriaEvents = shardToEventListMap.get(shard);
        // create the subcrit with the proper list of events
        Criteria newCrit = subcriteriaFactory.createSubcriteria(parentCriteria, criteriaEvents);
        // clear the list of events
        criteriaEvents.clear();
        // add it to our map
        shardToCriteriaMap.put(shard, newCrit);
      }
    };
  }

  public Criteria createCriteria(String associationPath)
      throws HibernateException {
    SubcriteriaFactory factory = new SubcriteriaFactoryImpl(associationPath);
    return createSubcriteria(factory);
  }

  public Criteria createCriteria(String associationPath, int joinType)
      throws HibernateException {
    SubcriteriaFactory factory = new SubcriteriaFactoryImpl(associationPath, joinType);
    return createSubcriteria(factory);
  }

  public Criteria createCriteria(String associationPath, String alias)
      throws HibernateException {
    SubcriteriaFactory factory = new SubcriteriaFactoryImpl(associationPath, alias);
    return createSubcriteria(factory);
  }

  public Criteria createCriteria(String associationPath, String alias,
      int joinType) throws HibernateException {
    SubcriteriaFactory factory = new SubcriteriaFactoryImpl(associationPath, alias, joinType);
    return createSubcriteria(factory);
  }

  public ShardedCriteria getParentCriteria() {
    return parent;
  }

  Map<Shard, Criteria> getShardToCriteriaMap() {
    return shardToCriteriaMap;
  }

  Map<Shard, List<CriteriaEvent>> getShardToEventListMap() {
    return shardToEventListMap;
  }

  interface SubcriteriaRegistrar {

    void establishSubcriteria(Criteria parentCriteria, SubcriteriaFactory subcriteriaFactory);

  }
}

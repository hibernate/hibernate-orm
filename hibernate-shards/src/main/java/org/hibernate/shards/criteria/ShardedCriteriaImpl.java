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
import org.hibernate.criterion.AvgProjection;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.shards.Shard;
import org.hibernate.shards.ShardOperation;
import org.hibernate.shards.strategy.access.ShardAccessStrategy;
import org.hibernate.shards.strategy.exit.ConcatenateListsExitStrategy;
import org.hibernate.shards.strategy.exit.FirstNonNullResultExitStrategy;
import org.hibernate.transform.ResultTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Concrete implementation of the {@link ShardedCriteria} interface.
 *
 * @author maxr@google.com (Max Ross)
 */
public class ShardedCriteriaImpl implements ShardedCriteria {
  
  // unique id for this ShardedCriteria
  private final CriteriaId criteriaId;

  // the shards we know about
  private final List<Shard> shards;

  // a factory that knows how to create actual Criteria objects
  private final CriteriaFactory criteriaFactory;

  // the shard access strategy we use when we execute the Criteria
  // across multiple shards
  private final ShardAccessStrategy shardAccessStrategy;

  // the criteria collector we use to process the results of executing
  // the Criteria across multiple shards
  private final ExitOperationsCriteriaCollector criteriaCollector;

  /**
   * Construct a ShardedCriteriaImpl
   *
   * @param criteriaId unique id for this ShardedCriteria
   * @param shards the shards that this ShardedCriteria is aware of
   * @param criteriaFactory factory that knows how to create concrete {@link Criteria} objects
   * @param shardAccessStrategy the access strategy we use when we execute this
   * ShardedCriteria across multiple shards.
   */
  public ShardedCriteriaImpl(
      CriteriaId criteriaId,
      List<Shard> shards,
      CriteriaFactory criteriaFactory,
      ShardAccessStrategy shardAccessStrategy) {
    this.criteriaId = criteriaId;
    this.shards = shards;
    this.criteriaFactory = criteriaFactory;
    this.shardAccessStrategy = shardAccessStrategy;
    this.criteriaCollector = new ExitOperationsCriteriaCollector();
    criteriaCollector.setSessionFactory(shards.get(0).getSessionFactoryImplementor());
  }

  public CriteriaId getCriteriaId() {
    return criteriaId;
  }

  public CriteriaFactory getCriteriaFactory() {
    return criteriaFactory;
  }

  /**
   * @return any Criteria, or null if we don't have one
   */
  private /*@Nullable*/ Criteria getSomeCriteria() {
    for (Shard shard : shards) {
      Criteria crit = shard.getCriteriaById(criteriaId);
      if (crit != null) {
        return crit;
      }
    }
    return null;
  }

  /**
   * @return any Criteria.  If no Criteria has been established we establish
   * one and return it.
   */
  private Criteria getOrEstablishSomeCriteria() {
    Criteria crit = getSomeCriteria();
    if(crit == null) {
      Shard shard = shards.get(0);
      crit = shard.establishCriteria(this);
    }
    return crit;
  }

  public String getAlias() {
    return getOrEstablishSomeCriteria().getAlias();
  }

  public Criteria setProjection(Projection projection) {
    criteriaCollector.addProjection(projection);
    if(projection instanceof AvgProjection) {
      setAvgProjection(projection);
    }
    // TODO - handle ProjectionList
    return this;
  }

  private void setAvgProjection(Projection projection) {
    // We need to modify the query to pull back not just the average but also
    // the count.  We'll do this by creating a ProjectionList with both the
    // average and the row count.
    ProjectionList projectionList = Projections.projectionList();
    projectionList.add(projection);
    projectionList.add(Projections.rowCount());
    CriteriaEvent event = new SetProjectionEvent(projectionList);
    for (Shard shard : shards) {
      if (shard.getCriteriaById(criteriaId) != null) {
        shard.getCriteriaById(criteriaId).setProjection(projectionList);
      } else {
        shard.addCriteriaEvent(criteriaId, event);
      }
    }
  }

  public Criteria add(Criterion criterion) {
    CriteriaEvent event = new AddCriterionEvent(criterion);
    for (Shard shard : shards) {
      if (shard.getCriteriaById(criteriaId) != null) {
        shard.getCriteriaById(criteriaId).add(criterion);
      } else {
        shard.addCriteriaEvent(criteriaId, event);
      }
    }
    return this;
  }

  public Criteria addOrder(Order order) {
    criteriaCollector.addOrder(order);
    return this;
  }

  public Criteria setFetchMode(String associationPath, FetchMode mode)
      throws HibernateException {
    CriteriaEvent event = new SetFetchModeEvent(associationPath, mode);
    for (Shard shard : shards) {
      if (shard.getCriteriaById(criteriaId) != null) {
        shard.getCriteriaById(criteriaId).setFetchMode(associationPath, mode);
      } else {
        shard.addCriteriaEvent(criteriaId, event);
      }
    }
    return this;
  }

  public Criteria setLockMode(LockMode lockMode) {
    CriteriaEvent event = new SetLockModeEvent(lockMode);
    for (Shard shard : shards) {
      if (shard.getCriteriaById(criteriaId) != null) {
        shard.getCriteriaById(criteriaId).setLockMode(lockMode);
      } else {
        shard.addCriteriaEvent(criteriaId, event);
      }
    }
    return this;
  }

  public Criteria setLockMode(String alias, LockMode lockMode) {
    CriteriaEvent event = new SetLockModeEvent(lockMode, alias);
    for (Shard shard : shards) {
      if (shard.getCriteriaById(criteriaId) != null) {
        shard.getCriteriaById(criteriaId).setLockMode(lockMode);
      } else {
        shard.addCriteriaEvent(criteriaId, event);
      }
    }
    return this;
  }

  public Criteria createAlias(String associationPath, String alias)
      throws HibernateException {
    CriteriaEvent event = new CreateAliasEvent(associationPath, alias);
    for (Shard shard : shards) {
      if (shard.getCriteriaById(criteriaId) != null) {
        shard.getCriteriaById(criteriaId).createAlias(associationPath, alias);
      } else {
        shard.addCriteriaEvent(criteriaId, event);
      }
    }
    return this;
  }

  public Criteria createAlias(String associationPath, String alias,
      int joinType) throws HibernateException {
    CriteriaEvent event = new CreateAliasEvent(associationPath, alias, joinType);
    for (Shard shard : shards) {
      if (shard.getCriteriaById(criteriaId) != null) {
        shard.getCriteriaById(criteriaId)
            .createAlias(associationPath, alias, joinType);
      } else {
        shard.addCriteriaEvent(criteriaId, event);
      }
    }
    return this;
  }

  private static final Iterable<CriteriaEvent> NO_CRITERIA_EVENTS =
      Collections.unmodifiableList(new ArrayList<CriteriaEvent>());

  /**
   * Creating sharded subcriteria is tricky.  We need to give the client a
   * reference to a ShardedSubcriteriaImpl (which to the client just looks like
   * a Criteria object).  Then, for each shard where the Criteria has already been
   * established we need to create the actual subcriteria, and for each shard
   * where the Criteria has not yet been established we need to register an
   * event that will create the Subcriteria when the Criteria is established.
   */
  private ShardedSubcriteriaImpl createSubcriteria(SubcriteriaFactory factory) {

    ShardedSubcriteriaImpl subcrit = new ShardedSubcriteriaImpl(shards, this);

    for (Shard shard : shards) {
      Criteria crit = shard.getCriteriaById(criteriaId);
      if(crit != null) {
        factory.createSubcriteria(crit, NO_CRITERIA_EVENTS);
      } else {
        CreateSubcriteriaEvent event =
            new CreateSubcriteriaEvent(factory, subcrit.getSubcriteriaRegistrar(shard));
        shard.addCriteriaEvent(criteriaId, event);
      }
    }
    return subcrit;
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

  public Criteria setResultTransformer(ResultTransformer resultTransformer) {
    CriteriaEvent event = new SetResultTransformerEvent(resultTransformer);
    for (Shard shard : shards) {
      if (shard.getCriteriaById(criteriaId) != null) {
        shard.getCriteriaById(criteriaId)
            .setResultTransformer(resultTransformer);
      } else {
        shard.addCriteriaEvent(criteriaId, event);
      }
    }
    return this;
  }

  public Criteria setMaxResults(int maxResults) {
    criteriaCollector.setMaxResults(maxResults);

    return this;
  }

  public Criteria setFirstResult(int firstResult) {
    criteriaCollector.setFirstResult(firstResult);
    return this;
  }

  public Criteria setFetchSize(int fetchSize) {
    CriteriaEvent event = new SetFetchSizeEvent(fetchSize);
    for (Shard shard : shards) {
      if (shard.getCriteriaById(criteriaId) != null) {
        shard.getCriteriaById(criteriaId).setFetchSize(fetchSize);
      } else {
        shard.addCriteriaEvent(criteriaId, event);
      }
    }
    return this;
  }

  public Criteria setTimeout(int timeout) {
    CriteriaEvent event = new SetTimeoutEvent(timeout);
    for (Shard shard : shards) {
      if (shard.getCriteriaById(criteriaId) != null) {
        shard.getCriteriaById(criteriaId).setTimeout(timeout);
      } else {
        shard.addCriteriaEvent(criteriaId, event);
      }
    }
    return this;
  }

  public Criteria setCacheable(boolean cacheable) {
    CriteriaEvent event = new SetCacheableEvent(cacheable);
    for (Shard shard : shards) {
      if (shard.getCriteriaById(criteriaId) != null) {
        shard.getCriteriaById(criteriaId).setCacheable(cacheable);
      } else {
        shard.addCriteriaEvent(criteriaId, event);
      }
    }
    return this;
  }

  public Criteria setCacheRegion(String cacheRegion) {
    CriteriaEvent event = new SetCacheRegionEvent(cacheRegion);
    for (Shard shard : shards) {
      if (shard.getCriteriaById(criteriaId) != null) {
        shard.getCriteriaById(criteriaId).setCacheRegion(cacheRegion);
      } else {
        shard.addCriteriaEvent(criteriaId, event);
      }
    }
    return this;
  }

  public Criteria setComment(String comment) {
    CriteriaEvent event = new SetCommentEvent(comment);
    for (Shard shard : shards) {
      if (shard.getCriteriaById(criteriaId) != null) {
        shard.getCriteriaById(criteriaId).setComment(comment);
      } else {
        shard.addCriteriaEvent(criteriaId, event);
      }
    }
    return this;
  }

  public Criteria setFlushMode(FlushMode flushMode) {
    CriteriaEvent event = new SetFlushModeEvent(flushMode);
    for (Shard shard : shards) {
      if (shard.getCriteriaById(criteriaId) != null) {
        shard.getCriteriaById(criteriaId).setFlushMode(flushMode);
      } else {
        shard.addCriteriaEvent(criteriaId, event);
      }
    }
    return this;
  }

  public Criteria setCacheMode(CacheMode cacheMode) {
    CriteriaEvent event = new SetCacheModeEvent(cacheMode);
    for (Shard shard : shards) {
      if (shard.getCriteriaById(criteriaId) != null) {
        shard.getCriteriaById(criteriaId).setCacheMode(cacheMode);
      } else {
        shard.addCriteriaEvent(criteriaId, event);
      }
    }
    return this;
  }

  /**
   * Unsupported.  This is a scope decision, not a technical decision.
   */
  public ScrollableResults scroll() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported.  This is a scope decision, not a technical decision.
   */
  public ScrollableResults scroll(ScrollMode scrollMode)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public List list() throws HibernateException {

    // build a shard operation and apply it across all shards
    ShardOperation<List<Object>> shardOp = new ShardOperation<List<Object>>() {
      public List<Object> execute(Shard shard) {
        shard.establishCriteria(ShardedCriteriaImpl.this);
        return shard.list(criteriaId);
      }

      public String getOperationName() {
        return "list()";
      }
    };
    /**
     * We don't support shard selection for criteria queries.  If you want
     * custom shards, create a ShardedSession with only the shards you want.
     * We're going to concatenate all our results and then use our
     * criteria collector to do post processing.
     */
    return
        shardAccessStrategy.apply(
            shards,
            shardOp,
            new ConcatenateListsExitStrategy(),
            criteriaCollector);
  }

  public Object uniqueResult() throws HibernateException {
    // build a shard operation and apply it across all shards
    ShardOperation<Object> shardOp = new ShardOperation<Object>() {
      public Object execute(Shard shard) {
        shard.establishCriteria(ShardedCriteriaImpl.this);
        return shard.uniqueResult(criteriaId);
      }

      public String getOperationName() {
        return "uniqueResult()";
      }
    };
    /**
     * We don't support shard selection for criteria queries.  If you want
     * custom shards, create a ShardedSession with only the shards you want.
     * We're going to return the first non-null result we get from a shard.
     */
    return
        shardAccessStrategy.apply(
            shards,
            shardOp,
            new FirstNonNullResultExitStrategy<Object>(),
            criteriaCollector);
  }
}

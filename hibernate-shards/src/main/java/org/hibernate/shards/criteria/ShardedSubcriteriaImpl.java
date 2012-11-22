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
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.shards.Shard;
import org.hibernate.shards.util.Lists;
import org.hibernate.shards.util.Maps;
import org.hibernate.shards.util.Preconditions;
import org.hibernate.sql.JoinType;
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

    private Boolean readOnly;

    /**
     * Construct a ShardedSubcriteriaImpl
     *
     * @param shards the shards that we're aware of
     * @param parent our parent
     */
    public ShardedSubcriteriaImpl(final List<Shard> shards, final ShardedCriteria parent) {

        Preconditions.checkNotNull(shards);
        Preconditions.checkNotNull(parent);
        Preconditions.checkArgument(!shards.isEmpty());

        this.shards = shards;
        this.parent = parent;

        // let's set up our maps
        for (final Shard shard : shards) {
            shardToCriteriaMap.put(shard, null);
            shardToEventListMap.put(shard, Lists.<CriteriaEvent>newArrayList());
        }
    }

    @Override
    public String getAlias() {
        return getOrEstablishSomeSubcriteria().getAlias();
    }

    @Override
    public Criteria setProjection(final Projection projection) {
        return setSubcriteriaEvent(new SetProjectionEvent(projection));
    }

    @Override
    public Criteria add(final Criterion criterion) {
        return setSubcriteriaEvent(new AddCriterionEvent(criterion));
    }

    @Override
    public Criteria addOrder(final Order order) {
        return setSubcriteriaEvent(new AddOrderEvent(order));
    }

    @Override
    public Criteria setFetchMode(final String associationPath, final FetchMode mode) throws HibernateException {
        return setSubcriteriaEvent(new SetFetchModeEvent(associationPath, mode));
    }

    @Override
    public Criteria setLockMode(final LockMode lockMode) {
        return setSubcriteriaEvent(new SetLockModeEvent(lockMode));
    }

    @Override
    public Criteria setLockMode(final String alias, final LockMode lockMode) {
        return setSubcriteriaEvent(new SetLockModeEvent(lockMode, alias));
    }

    @Override
    public Criteria createAlias(final String associationPath, final String alias) throws HibernateException {
        return setSubcriteriaEvent(new CreateAliasEvent(associationPath, alias));
    }

    @Override
    public Criteria createAlias(final String associationPath, final String alias, final JoinType joinType)
            throws HibernateException {
        return setSubcriteriaEvent(new CreateAliasEvent(associationPath, alias, joinType));
    }

    @Override
    @Deprecated
    public Criteria createAlias(final String associationPath, final String alias, final int joinType)
            throws HibernateException {
        return createAlias(associationPath, alias, JoinType.parse(joinType));
    }

    @Override
    public Criteria createAlias(final String associationPath, final String alias, final JoinType joinType,
                                final Criterion withClause) throws HibernateException {
        return setSubcriteriaEvent(new CreateAliasEvent(associationPath, alias, joinType));
    }

    @Override
    @Deprecated
    public Criteria createAlias(final String associationPath, final String alias, final int joinType,
                                final Criterion withClause) throws HibernateException {
        return createAlias(associationPath, alias, JoinType.parse(joinType), withClause);
    }

    @Override
    public Criteria setResultTransformer(final ResultTransformer resultTransformer) {
        return setSubcriteriaEvent(new SetResultTransformerEvent(resultTransformer));
    }

    /**
     * TODO(maxr)
     * This clearly isn't what people want.  We should be building an
     * exit strategy that returns once we've accumulated maxResults
     * across _all_ shards, not each shard.
     */
    @Override
    public Criteria setMaxResults(final int maxResults) {
        return setSubcriteriaEvent(new SetMaxResultsEvent(maxResults));
    }

    @Override
    public Criteria setFirstResult(final int firstResult) {
        return setSubcriteriaEvent(new SetFirstResultEvent(firstResult));
    }

    @Override
    public boolean isReadOnlyInitialized() {
        return readOnly != null;
    }

    @Override
    public boolean isReadOnly() {
        if ( ! isReadOnlyInitialized() && (shards == null || shards.isEmpty()) ) {
            throw new IllegalStateException(
                    "cannot determine readOnly/modifiable setting when it is not initialized and is not initialized and getSession() == null"
            );
        }

        boolean defaultReadOnly = true;
        for (final Shard shard : shards) {
            final SessionImplementor session = (SessionImplementor)shard.getSessionFactoryImplementor().getCurrentSession();
            if (session != null) {
                defaultReadOnly &= session.getPersistenceContext().isDefaultReadOnly();
            }
        }

        return ( isReadOnlyInitialized() ? readOnly : defaultReadOnly );
    }

    @Override
    public Criteria setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    @Override
    public Criteria setFetchSize(final int fetchSize) {
        return setSubcriteriaEvent(new SetFetchSizeEvent(fetchSize));
    }

    @Override
    public Criteria setTimeout(final int timeout) {
        return setSubcriteriaEvent(new SetTimeoutEvent(timeout));
    }

    @Override
    public Criteria setCacheable(final boolean cacheable) {
        return setSubcriteriaEvent(new SetCacheableEvent(cacheable));
    }

    @Override
    public Criteria setCacheRegion(final String cacheRegion) {
        return setSubcriteriaEvent(new SetCacheRegionEvent(cacheRegion));
    }

    @Override
    public Criteria setComment(final String comment) {
        return setSubcriteriaEvent(new SetCommentEvent(comment));
    }

    @Override
    public Criteria setFlushMode(final FlushMode flushMode) {
        return setSubcriteriaEvent(new SetFlushModeEvent(flushMode));
    }

    @Override
    public Criteria setCacheMode(final CacheMode cacheMode) {
        return setSubcriteriaEvent(new SetCacheModeEvent(cacheMode));
    }

    @Override
    public List list() throws HibernateException {
        // pass through to the parent
        return getParentCriteria().list();
    }

    @Override
    public ScrollableResults scroll() throws HibernateException {
        // pass through to the parent
        return getParentCriteria().scroll();
    }

    @Override
    public ScrollableResults scroll(final ScrollMode scrollMode) throws HibernateException {
        // pass through to the parent
        return getParentCriteria().scroll(scrollMode);
    }

    @Override
    public Object uniqueResult() throws HibernateException {
        // pass through to the parent
        return getParentCriteria().uniqueResult();
    }

    /**
     * @return Returns an actual Criteria object, or null if none have been allocated.
     */
    private /*@Nullable*/ Criteria getSomeSubcriteria() {
        for (final Criteria crit : shardToCriteriaMap.values()) {
            if (crit != null) {
                return crit;
            }
        }
        return null;
    }

    /**
     * @return Returns an actual Criteria object.  If no actual Criteria object
     *         has been allocated, allocate one and return it.
     */
    private Criteria getOrEstablishSomeSubcriteria() {
        final Criteria crit = getSomeSubcriteria();
        if (crit == null) {
            Shard shard = shards.get(0);
            // this should trigger the creation of all subcriteria for the parent
            shard.establishCriteria(parent);
        }
        return getSomeSubcriteria();
    }

    private ShardedSubcriteriaImpl createSubcriteria(final SubcriteriaFactory factory) {
        // first build our sharded subcrit
        final ShardedSubcriteriaImpl subcrit = new ShardedSubcriteriaImpl(shards, parent);
        for (final Shard shard : shards) {
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

    @Override
    public Criteria createCriteria(final String associationPath) throws HibernateException {
        final SubcriteriaFactory factory = new SubcriteriaFactoryImpl(associationPath);
        return createSubcriteria(factory);
    }

    @Override
    public Criteria createCriteria(final String associationPath, final JoinType joinType) throws HibernateException {
        final SubcriteriaFactory factory = new SubcriteriaFactoryImpl(associationPath, joinType);
        return createSubcriteria(factory);
    }

    @Deprecated
    @Override
    public Criteria createCriteria(final String associationPath, final int joinType) throws HibernateException {
        return createCriteria(associationPath, JoinType.parse(joinType));
    }

    @Override
    public Criteria createCriteria(final String associationPath, final String alias) throws HibernateException {
        final SubcriteriaFactory factory = new SubcriteriaFactoryImpl(associationPath, alias);
        return createSubcriteria(factory);
    }

    @Override
    public Criteria createCriteria(final String associationPath, final String alias, final JoinType joinType)
            throws HibernateException {
        final SubcriteriaFactory factory = new SubcriteriaFactoryImpl(associationPath, alias, joinType);
        return createSubcriteria(factory);
    }

    @Deprecated
    @Override
    public Criteria createCriteria(final String associationPath, final String alias, final int joinType)
            throws HibernateException {
        return createCriteria(associationPath, alias, JoinType.parse(joinType));
    }

    @Override
    public Criteria createCriteria(final String associationPath, final String alias, final JoinType joinType,
                                   final Criterion withClause) throws HibernateException {
        final SubcriteriaFactory factory = new SubcriteriaFactoryImpl(associationPath, alias, joinType, withClause);
        return createSubcriteria(factory);
    }

    @Deprecated
    @Override
    public Criteria createCriteria(final String associationPath, final String alias, final int joinType,
                                   final Criterion withClause) throws HibernateException {
        return createCriteria(associationPath, alias, JoinType.parse(joinType), withClause);
    }

    @Override
    public ShardedCriteria getParentCriteria() {
        return parent;
    }

    private ShardedSubcriteriaImpl setSubcriteriaEvent(final CriteriaEvent event) {
        for (final Shard shard : shards) {
            if (shardToCriteriaMap.get(shard) != null) {
                event.onEvent(shardToCriteriaMap.get(shard));
            } else {
                shardToEventListMap.get(shard).add(event);
            }
        }
        return this;
    }

    SubcriteriaRegistrar getSubcriteriaRegistrar(final Shard shard) {
        return new SubcriteriaRegistrar() {

            public void establishSubcriteria(final Criteria parentCriteria, final SubcriteriaFactory subcriteriaFactory) {
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

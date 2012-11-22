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

package org.hibernate.shards.query;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.shards.Shard;
import org.hibernate.shards.ShardOperation;
import org.hibernate.shards.strategy.access.ShardAccessStrategy;
import org.hibernate.shards.strategy.exit.ConcatenateListsExitStrategy;
import org.hibernate.shards.strategy.exit.FirstNonNullResultExitStrategy;
import org.hibernate.shards.util.Preconditions;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Concrete implementation of ShardedQuery provided by Hibernate Shards. This
 * implementation introduces limits to the HQL language; mostly around
 * limits and aggregation. Its approach is simply to execute the query on
 * each shard and compile the results in a list, or if a unique result is
 * desired, the fist non-null result is returned.
 * <p/>
 * The setFoo methods are implemented using a set of classes that implement
 * the QueryEvent interface and are called SetFooEvent. These query events
 * are used to call setFoo with the appropriate arguments on each Query that
 * is executed on a shard.
 *
 * @author Maulik Shah
 * @see org.hibernate.shards.query.QueryEvent
 */
public class ShardedQueryImpl implements ShardedQuery {

    private final QueryId queryId;
    private final List<Shard> shards;
    private final QueryFactory queryFactory;
    private final ShardAccessStrategy shardAccessStrategy;

    /**
     * The queryCollector is not used in ShardedQueryImpl as it would require
     * this implementation to parse the query string and extract which exit
     * operations would be appropriate. This member is a place holder for
     * future development.
     */
    private final ExitOperationsQueryCollector queryCollector;

    /**
     * Constructor for ShardedQueryImpl
     *
     * @param queryId             the id of the query
     * @param shards              list of shards on which this query will be executed
     * @param queryFactory        factory that knows how to create the actual query we'll execute
     * @param shardAccessStrategy the shard strategy for this query
     */
    public ShardedQueryImpl(final QueryId queryId,
                            final List<Shard> shards,
                            final QueryFactory queryFactory,
                            final ShardAccessStrategy shardAccessStrategy) {

        this.queryId = queryId;
        this.shards = shards;
        this.queryFactory = queryFactory;
        this.shardAccessStrategy = shardAccessStrategy;
        this.queryCollector = new ExitOperationsQueryCollector();

        Preconditions.checkState(!shards.isEmpty());
        for (Shard shard : shards) {
            Preconditions.checkNotNull(shard);
        }
    }

    @Override
    public QueryId getQueryId() {
        return queryId;
    }

    @Override
    public QueryFactory getQueryFactory() {
        return queryFactory;
    }

    @Override
    public String getQueryString() {
        return getOrEstablishSomeQuery().getQueryString();
    }

    @Override
    public Type[] getReturnTypes() throws HibernateException {
        return getOrEstablishSomeQuery().getReturnTypes();
    }

    @Override
    public String[] getReturnAliases() throws HibernateException {
        return getOrEstablishSomeQuery().getReturnAliases();
    }

    @Override
    public String[] getNamedParameters() throws HibernateException {
        return getOrEstablishSomeQuery().getNamedParameters();
    }

    /**
     * This method currently wraps list().
     * <p/>
     * {@inheritDoc}
     *
     * @return an iterator over the results of the query
     * @throws HibernateException
     */
    @Override
    public Iterator iterate() throws HibernateException {
        /**
         * TODO(maulik) Hibernate in Action says these two methods are equivalent
         * in what the content that they return but are implemented differently.
         * We should figure out the difference and implement correctly.
         */
        return list().iterator();
    }

    /**
     * Scrolling is unsupported. Current implementation throws an
     * UnsupportedOperationException. A dumb implementation of scroll might be
     * possible; however it would provide no performance benefit. An intelligent
     * implementation would require re-querying shards frequently and a
     * deterministic way to complie results.
     */
    @Override
    public ScrollableResults scroll() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    /**
     * Scrolling is unsupported. Current implementation throws an
     * UnsupportedOperationException. A dumb implementation of scroll might be
     * possible; however it would provide no performance benefit. An intelligent
     * implementation would require re-querying shards frequently and a
     * deterministic way to complie results.
     */
    @Override
    public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    /**
     * The implementation executes the query on each shard and concatenates the
     * results.
     * @return a list containing the concatenated results of executing the
     *         query on all shards
     * @throws HibernateException
     */
    @Override
    public List list() throws HibernateException {
        final ShardOperation<List<Object>> shardOp = new ShardOperation<List<Object>>() {
            public List<Object> execute(final Shard shard) {
                shard.establishQuery(ShardedQueryImpl.this);
                return shard.list(queryId);
            }

            public String getOperationName() {
                return "list()";
            }
        };

        /**
         * We don't support shard selection for HQL queries.  If you want
         * custom shards, create a ShardedSession with only the shards you want.
         */
        return shardAccessStrategy.apply(
                shards,
                shardOp,
                new ConcatenateListsExitStrategy(),
                queryCollector);
    }

    /**
     * The implementation executes the query on each shard and returns the first
     * non-null result.
     * <p/>
     * {@inheritDoc}
     *
     * @return the first non-null result, or null if no non-null result found
     * @throws HibernateException
     */
    @Override
    public Object uniqueResult() throws HibernateException {
        final ShardOperation<Object> shardOp = new ShardOperation<Object>() {
            public Object execute(final Shard shard) {
                shard.establishQuery(ShardedQueryImpl.this);
                return shard.uniqueResult(queryId);
            }

            public String getOperationName() {
                return "uniqueResult()";
            }
        };

        /**
         * We don't support shard selection for HQL queries.  If you want
         * custom shards, create a ShardedSession with only the shards you want.
         */
        return shardAccessStrategy.apply(
                shards,
                shardOp,
                new FirstNonNullResultExitStrategy<Object>(),
                queryCollector);
    }

    /**
     * @throws HibernateException
     */
    @Override
    public int executeUpdate() throws HibernateException {

        final ShardOperation<List<Object>> shardOp = new ShardOperation<List<Object>>() {

            @Override
            public List<Object> execute(Shard shard) {
                shard.establishQuery(ShardedQueryImpl.this);
                int tmp = shard.executeUpdate(queryId);
                return Collections.singletonList((Object) tmp);
            }

            @Override
            public String getOperationName() {
                return "executeUpdate()";
            }
        };

        final List<Object> rets = shardAccessStrategy.apply(shards, shardOp, new ConcatenateListsExitStrategy(),
                queryCollector);

        int sum = 0;

        for (final Object i : rets) {
            sum += (Integer) i;
        }

        return sum;
    }

    @Override
    public Query setMaxResults(int maxResults) {
        queryCollector.setMaxResults(maxResults);
        return this;
    }

    @Override
    public Query setFirstResult(int firstResult) {
        queryCollector.setFirstResult(firstResult);
        return this;
    }

    @Override
    public boolean isReadOnly() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Query setReadOnly(final boolean readOnly) {
        return setQueryEvent(new SetReadOnlyEvent(readOnly));
    }

    @Override
    public Query setCacheable(final boolean cacheable) {
        return setQueryEvent(new SetCacheableEvent(cacheable));
    }

    @Override
    public Query setCacheRegion(final String cacheRegion) {
        return setQueryEvent(new SetCacheRegionEvent(cacheRegion));
    }

    @Override
    public Query setTimeout(final int timeout) {
        return setQueryEvent(new SetTimeoutEvent(timeout));
    }

    @Override
    public Query setFetchSize(final int fetchSize) {
        return setQueryEvent(new SetFetchSizeEvent(fetchSize));
    }

    @Override
    public Query setLockOptions(final LockOptions lockOptions) {
        return setQueryEvent(new SetLockOptionsEvent(lockOptions));
    }

    @Override
    public Query setLockMode(final String alias, final LockMode lockMode) {
        return setQueryEvent(new SetLockModeEvent(alias, lockMode));
    }

    @Override
    public Query setComment(final String comment) {
        return setQueryEvent(new SetCommentEvent(comment));
    }

    @Override
    public Query setFlushMode(final FlushMode flushMode) {
        return setQueryEvent(new SetFlushModeEvent(flushMode));
    }

    @Override
    public Query setCacheMode(final CacheMode cacheMode) {
        return setQueryEvent(new SetCacheModeEvent(cacheMode));
    }

    @Override
    public Query setParameter(final int position, final Object val, final Type type) {
        return setQueryEvent(new SetParameterEvent(position, val, type));
    }

    @Override
    public Query setParameter(final String name, final Object val, final Type type) {
        return setQueryEvent(new SetParameterEvent(name, val, type));
    }

    @Override
    public Query setParameter(final int position, final Object val) throws HibernateException {
        return setQueryEvent(new SetParameterEvent(position, val));
    }

    @Override
    public Query setParameter(final String name, final Object val) throws HibernateException {
        return setQueryEvent(new SetParameterEvent(name, val));
    }

    @Override
    public Query setParameters(final Object[] values, final Type[] types) throws HibernateException {
        return setQueryEvent(new SetParametersEvent(values, types));
    }

    @Override
    public Query setParameterList(final String name, final Collection vals, final Type type) throws HibernateException {
        return setQueryEvent(new SetParameterListEvent(name, vals, type));
    }

    @Override
    public Query setParameterList(final String name, final Collection vals) throws HibernateException {
        return setQueryEvent(new SetParameterListEvent(name, vals));
    }

    @Override
    public Query setParameterList(final String name, final Object[] vals, final Type type) throws HibernateException {
        return setQueryEvent(new SetParameterListEvent(name, vals, type));
    }

    @Override
    public Query setParameterList(final String name, final Object[] vals) throws HibernateException {
        return setQueryEvent(new SetParameterListEvent(name, vals));
    }

    @Override
    public Query setProperties(final Object bean) throws HibernateException {
        return setQueryEvent(new SetPropertiesEvent(bean));
    }

    @Override
    public Query setString(final int position, final String val) {
        return setQueryEvent(new SetStringEvent(position, val));
    }

    @Override
    public Query setCharacter(final int position, final char val) {
        return setQueryEvent(new SetCharacterEvent(position, val));
    }

    @Override
    public Query setBoolean(final int position, final boolean val) {
        return setQueryEvent(new SetBooleanEvent(position, val));
    }

    @Override
    public Query setByte(int position, byte val) {
        return setQueryEvent(new SetByteEvent(position, val));
    }

    @Override
    public Query setShort(final int position, final short val) {
        return setQueryEvent(new SetShortEvent(position, val));
    }

    @Override
    public Query setInteger(final int position, final int val) {
        return setQueryEvent(new SetIntegerEvent(position, val));
    }

    @Override
    public Query setLong(final int position, final long val) {
        return setQueryEvent(new SetLongEvent(position, val));
    }

    @Override
    public Query setFloat(final int position, final float val) {
        return setQueryEvent(new SetFloatEvent(position, val));
    }

    @Override
    public Query setDouble(final int position, final double val) {
        return setQueryEvent(new SetDoubleEvent(position, val));
    }

    @Override
    public Query setBinary(final int position, final byte[] val) {
        return setQueryEvent(new SetBinaryEvent(position, val));
    }

    @Override
    public Query setText(final int position, final String val) {
        final QueryEvent event = new SetTextEvent(position, val);
        return setQueryEvent(event);
    }

    @Override
    public Query setSerializable(final int position, final Serializable val) {
        return setQueryEvent(new SetSerializableEvent(position, val));
    }

    @Override
    public Query setLocale(int position, Locale locale) {
        return setQueryEvent(new SetLocaleEvent(position, locale));
    }

    @Override
    public Query setBigDecimal(final int position, final BigDecimal number) {
        return setQueryEvent(new SetBigDecimalEvent(position, number));
    }

    @Override
    public Query setBigInteger(final int position, final BigInteger number) {
        return setQueryEvent(new SetBigIntegerEvent(position, number));
    }

    @Override
    public Query setDate(final int position, final Date date) {
        return setQueryEvent(new SetDateEvent(position, date));
    }

    @Override
    public Query setTime(final int position, final Date date) {
        return setQueryEvent(new SetTimeEvent(position, date));
    }

    @Override
    public Query setTimestamp(final int position, final Date date) {
        return setQueryEvent(new SetTimestampEvent(position, date));
    }

    @Override
    public Query setCalendar(final int position, final Calendar calendar) {
        return setQueryEvent(new SetCalendarEvent(position, calendar));
    }

    @Override
    public Query setCalendarDate(int position, Calendar calendar) {
        return setQueryEvent(new SetCalendarDateEvent(position, calendar));
    }

    @Override
    public Query setString(String name, String val) {
        return setQueryEvent(new SetStringEvent(name, val));
    }

    @Override
    public Query setCharacter(final String name, final char val) {
        return setQueryEvent(new SetCharacterEvent(name, val));
    }

    public Query setBoolean(String name, boolean val) {
        return setQueryEvent(new SetBooleanEvent(name, val));
    }

    @Override
    public Query setByte(String name, byte val) {
        return setQueryEvent(new SetByteEvent(name, val));
    }

    @Override
    public Query setShort(String name, short val) {
        return setQueryEvent(new SetShortEvent(name, val));
    }

    @Override
    public Query setInteger(String name, int val) {
        return setQueryEvent(new SetIntegerEvent(name, val));
    }

    @Override
    public Query setLong(String name, long val) {
        return setQueryEvent(new SetLongEvent(name, val));
    }

    @Override
    public Query setFloat(String name, float val) {
        return setQueryEvent(new SetFloatEvent(name, val));
    }

    @Override
    public Query setDouble(String name, double val) {
        return setQueryEvent(new SetDoubleEvent(name, val));
    }

    @Override
    public Query setBinary(String name, byte[] val) {
        return setQueryEvent(new SetBinaryEvent(name, val));
    }

    @Override
    public Query setText(String name, String val) {
        return setQueryEvent(new SetTextEvent(name, val));
    }

    @Override
    public Query setSerializable(String name, Serializable val) {
        return setQueryEvent(new SetSerializableEvent(name, val));
    }

    @Override
    public Query setLocale(final String name, final Locale locale) {
        return setQueryEvent(new SetLocaleEvent(name, locale));
    }

    @Override
    public Query setBigDecimal(final String name, final BigDecimal number) {
        return setQueryEvent(new SetBigDecimalEvent(name, number));
    }

    @Override
    public Query setBigInteger(String name, BigInteger number) {
        return setQueryEvent(new SetBigIntegerEvent(name, number));
    }

    @Override
    public Query setDate(final String name, final Date date) {
        return setQueryEvent(new SetDateEvent(name, date));
    }

    @Override
    public Query setTime(final String name, final Date date) {
        return setQueryEvent(new SetTimeEvent(name, date));
    }

    @Override
    public Query setTimestamp(final String name, final Date date) {
        return setQueryEvent(new SetTimestampEvent(name, date));
    }

    @Override
    public Query setCalendar(final String name, final Calendar calendar) {
        return setQueryEvent(new SetCalendarEvent(name, calendar));
    }

    @Override
    public Query setCalendarDate(final String name, final Calendar calendar) {
        return setQueryEvent(new SetCalendarDateEvent(name, calendar));
    }

    @Override
    public Query setEntity(final int position, final Object val) {
        return setQueryEvent(new SetEntityEvent(position, val));
    }

    @Override
    public Query setEntity(final String name, final Object val) {
        return setQueryEvent(new SetEntityEvent(name, val));
    }

    @Override
    public Query setResultTransformer(final ResultTransformer transformer) {
        return setQueryEvent(new SetResultTransformerEvent(transformer));
    }

    @Override
    public Query setProperties(final Map map) throws HibernateException {
        final QueryEvent event = new SetPropertiesEvent(map);
        return setQueryEvent(event);
    }

    private Query setQueryEvent(final QueryEvent queryEvent) throws HibernateException {
        for (final Shard shard : shards) {
            if (shard.getQueryById(queryId) != null) {
                queryEvent.onEvent(shard.getQueryById(queryId));
            } else {
                shard.addQueryEvent(queryId, queryEvent);
            }
        }
        return this;
    }

    private Query getSomeQuery() {
        for (final Shard shard : shards) {
            final Query query = shard.getQueryById(queryId);
            if (query != null) {
                return query;
            }
        }
        return null;
    }

    private Query getOrEstablishSomeQuery() {
        Query query = getSomeQuery();
        if (query == null) {
            final Shard shard = shards.get(0);
            query = shard.establishQuery(this);
        }
        return query;
    }
}

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
import org.hibernate.sql.JoinType;
import org.hibernate.transform.ResultTransformer;

import java.util.List;

/**
 * @author maxr@google.com (Max Ross)
 */
public class ShardedCriteriaDefaultMock implements ShardedCriteria {

    @Override
    public CriteriaId getCriteriaId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CriteriaFactory getCriteriaFactory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getAlias() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria setProjection(Projection projection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria add(Criterion criterion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria addOrder(Order order) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria setFetchMode(String associationPath, FetchMode mode) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria setLockMode(LockMode lockMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria setLockMode(String alias, LockMode lockMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria createAlias(String associationPath, String alias) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria createAlias(String associationPath, String alias, JoinType joinType) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Criteria createAlias(String associationPath, String alias, int joinType) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria createAlias(String associationPath, String alias, JoinType joinType, Criterion withClause) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Criteria createAlias(String associationPath, String alias, int joinType, Criterion withClause) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria createCriteria(String associationPath) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria createCriteria(String associationPath, JoinType joinType) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Criteria createCriteria(String associationPath, int joinType) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria createCriteria(String associationPath, String alias) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria createCriteria(String associationPath, String alias, JoinType joinType) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Criteria createCriteria(String associationPath, String alias, int joinType) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria createCriteria(String associationPath, String alias, JoinType joinType, Criterion withClause) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Criteria createCriteria(String associationPath, String alias, int joinType, Criterion withClause) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria setResultTransformer(ResultTransformer resultTransformer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria setMaxResults(int maxResults) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria setFirstResult(int firstResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReadOnlyInitialized() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReadOnly() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria setReadOnly(boolean readOnly) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria setFetchSize(int fetchSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria setTimeout(int timeout) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria setCacheable(boolean cacheable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria setCacheRegion(String cacheRegion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria setComment(String comment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria setFlushMode(FlushMode flushMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria setCacheMode(CacheMode cacheMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List list() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScrollableResults scroll() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object uniqueResult() throws HibernateException {
        throw new UnsupportedOperationException();
    }
}

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
import org.hibernate.transform.ResultTransformer;

import java.util.List;

/**
 * @author maxr@google.com (Max Ross)
 */
public class ShardedCriteriaDefaultMock implements ShardedCriteria {

  public CriteriaId getCriteriaId() {
    throw new UnsupportedOperationException();
  }

  public CriteriaFactory getCriteriaFactory() {
    throw new UnsupportedOperationException();
  }

  public String getAlias() {
    throw new UnsupportedOperationException();
  }

  public Criteria setProjection(Projection projection) {
    throw new UnsupportedOperationException();
  }

  public Criteria add(Criterion criterion) {
    throw new UnsupportedOperationException();
  }

  public Criteria addOrder(Order order) {
    throw new UnsupportedOperationException();
  }

  public Criteria setFetchMode(String associationPath, FetchMode mode)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Criteria setLockMode(LockMode lockMode) {
    throw new UnsupportedOperationException();
  }

  public Criteria setLockMode(String alias, LockMode lockMode) {
    throw new UnsupportedOperationException();
  }

  public Criteria createAlias(String associationPath, String alias)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Criteria createAlias(String associationPath, String alias,
      int joinType) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Criteria createCriteria(String associationPath)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Criteria createCriteria(String associationPath, int joinType)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Criteria createCriteria(String associationPath, String alias)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Criteria createCriteria(String associationPath, String alias,
      int joinType) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Criteria setResultTransformer(ResultTransformer resultTransformer) {
    throw new UnsupportedOperationException();
  }

  public Criteria setMaxResults(int maxResults) {
    throw new UnsupportedOperationException();
  }

  public Criteria setFirstResult(int firstResult) {
    throw new UnsupportedOperationException();
  }

  public Criteria setFetchSize(int fetchSize) {
    throw new UnsupportedOperationException();
  }

  public Criteria setTimeout(int timeout) {
    throw new UnsupportedOperationException();
  }

  public Criteria setCacheable(boolean cacheable) {
    throw new UnsupportedOperationException();
  }

  public Criteria setCacheRegion(String cacheRegion) {
    throw new UnsupportedOperationException();
  }

  public Criteria setComment(String comment) {
    throw new UnsupportedOperationException();
  }

  public Criteria setFlushMode(FlushMode flushMode) {
    throw new UnsupportedOperationException();
  }

  public Criteria setCacheMode(CacheMode cacheMode) {
    throw new UnsupportedOperationException();
  }

  public List list() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public ScrollableResults scroll() throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public ScrollableResults scroll(ScrollMode scrollMode)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object uniqueResult() throws HibernateException {
    throw new UnsupportedOperationException();
  }
}

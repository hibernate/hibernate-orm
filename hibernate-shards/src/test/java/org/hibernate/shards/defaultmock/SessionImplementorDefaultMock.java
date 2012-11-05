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

package org.hibernate.shards.defaultmock;

import org.hibernate.CacheMode;
import org.hibernate.EntityMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.query.sql.NativeSQLQuerySpecification;
import org.hibernate.event.EventListeners;
import org.hibernate.impl.CriteriaImpl;
import org.hibernate.jdbc.Batcher;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author maxr@google.com (Max Ross)
 */
public class SessionImplementorDefaultMock implements SessionImplementor {

  public void setAutoClear(boolean enabled) {
    throw new UnsupportedOperationException();
  }

  public Interceptor getInterceptor() {
    throw new UnsupportedOperationException();
  }

  public boolean isTransactionInProgress() {
    throw new UnsupportedOperationException();
  }

  public void initializeCollection(PersistentCollection collection,
      boolean writing) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object internalLoad(String entityName, Serializable id, boolean eager,
      boolean nullable) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object immediateLoad(String entityName, Serializable id)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public long getTimestamp() {
    throw new UnsupportedOperationException();
  }

  public SessionFactoryImplementor getFactory() {
    throw new UnsupportedOperationException();
  }

  public Batcher getBatcher() {
    throw new UnsupportedOperationException();
  }

  public List list(String query, QueryParameters queryParameters)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Iterator iterate(String query, QueryParameters queryParameters)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public ScrollableResults scroll(String query, QueryParameters queryParameters)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public ScrollableResults scroll(CriteriaImpl criteria,
      ScrollMode scrollMode) {
    throw new UnsupportedOperationException();
  }

  public List list(CriteriaImpl criteria) {
    throw new UnsupportedOperationException();
  }

  public List listFilter(Object collection, String filter,
      QueryParameters queryParameters) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Iterator iterateFilter(Object collection, String filter,
      QueryParameters queryParameters) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public EntityPersister getEntityPersister(String entityName, Object object)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object getEntityUsingInterceptor(EntityKey key)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public void afterTransactionCompletion(boolean successful, Transaction tx) {
    throw new UnsupportedOperationException();
  }

  public void beforeTransactionCompletion(Transaction tx) {
    throw new UnsupportedOperationException();
  }

  public Serializable getContextEntityIdentifier(Object object) {
    throw new UnsupportedOperationException();
  }

  public String bestGuessEntityName(Object object) {
    throw new UnsupportedOperationException();
  }

  public String guessEntityName(Object entity) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object instantiate(String entityName, Serializable id)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public List listCustomQuery(CustomQuery customQuery,
      QueryParameters queryParameters) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public ScrollableResults scrollCustomQuery(CustomQuery customQuery,
      QueryParameters queryParameters) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public List list(NativeSQLQuerySpecification spec,
      QueryParameters queryParameters) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public ScrollableResults scroll(NativeSQLQuerySpecification spec,
      QueryParameters queryParameters) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public Object getFilterParameterValue(String filterParameterName) {
    throw new UnsupportedOperationException();
  }

  public Type getFilterParameterType(String filterParameterName) {
    throw new UnsupportedOperationException();
  }

  public Map getEnabledFilters() {
    throw new UnsupportedOperationException();
  }

  public int getDontFlushFromFind() {
    throw new UnsupportedOperationException();
  }

  public EventListeners getListeners() {
    throw new UnsupportedOperationException();
  }

  public PersistenceContext getPersistenceContext() {
    throw new UnsupportedOperationException();
  }

  public int executeUpdate(String query, QueryParameters queryParameters)
      throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public int executeNativeUpdate(NativeSQLQuerySpecification specification,
      QueryParameters queryParameters) throws HibernateException {
    throw new UnsupportedOperationException();
  }

  public EntityMode getEntityMode() {
    throw new UnsupportedOperationException();
  }

  public CacheMode getCacheMode() {
    throw new UnsupportedOperationException();
  }

  public void setCacheMode(CacheMode cm) {
    throw new UnsupportedOperationException();
  }

  public boolean isOpen() {
    throw new UnsupportedOperationException();
  }

  public boolean isConnected() {
    throw new UnsupportedOperationException();
  }

  public FlushMode getFlushMode() {
    throw new UnsupportedOperationException();
  }

  public void setFlushMode(FlushMode fm) {
    throw new UnsupportedOperationException();
  }

  public Connection connection() {
    throw new UnsupportedOperationException();
  }

  public void flush() {
    throw new UnsupportedOperationException();
  }

  public Query getNamedQuery(String name) {
    throw new UnsupportedOperationException();
  }

  public Query getNamedSQLQuery(String name) {
    throw new UnsupportedOperationException();
  }

  public boolean isEventSource() {
    throw new UnsupportedOperationException();
  }

  public void afterScrollOperation() {
    throw new UnsupportedOperationException();
  }

  public void setFetchProfile(String name) {
    throw new UnsupportedOperationException();
  }

  public String getFetchProfile() {
    throw new UnsupportedOperationException();
  }

  public JDBCContext getJDBCContext() {
    throw new UnsupportedOperationException();
  }

  public boolean isClosed() {
    throw new UnsupportedOperationException();
  }
}

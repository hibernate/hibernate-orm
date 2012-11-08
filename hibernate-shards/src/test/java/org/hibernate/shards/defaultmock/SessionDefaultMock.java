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

import org.hibernate.*;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.stat.SessionStatistics;

import java.io.Serializable;
import java.sql.Connection;

/**
 * @author maxr@google.com (Max Ross)
 */
public class SessionDefaultMock implements Session {

    @Override
    public SharedSessionBuilder sessionWithOptions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFlushMode(FlushMode flushMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FlushMode getFlushMode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCacheMode(CacheMode cacheMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CacheMode getCacheMode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SessionFactory getSessionFactory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection close() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancelQuery() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpen() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConnected() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDirty() throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDefaultReadOnly() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDefaultReadOnly(boolean readOnly) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Serializable getIdentifier(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void evict(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Object load(Class theClass, Serializable id, LockMode lockMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object load(Class theClass, Serializable id, LockOptions lockOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Object load(String entityName, Serializable id, LockMode lockMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object load(String entityName, Serializable id, LockOptions lockOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object load(Class theClass, Serializable id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object load(String entityName, Serializable id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void load(Object object, Serializable id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replicate(Object object, ReplicationMode replicationMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replicate(String entityName, Object object, ReplicationMode replicationMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Serializable save(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Serializable save(String entityName, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveOrUpdate(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveOrUpdate(String entityName, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(String entityName, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object merge(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object merge(String entityName, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void persist(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void persist(String entityName, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String entityName, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void lock(Object object, LockMode lockMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void lock(String entityName, Object object, LockMode lockMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LockRequest buildLockRequest(LockOptions lockOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(String entityName, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public void refresh(Object object, LockMode lockMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(Object object, LockOptions lockOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(String entityName, Object object, LockOptions lockOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LockMode getCurrentLockMode(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Query createFilter(Object collection, String queryString) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(Class clazz, Serializable id) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Object get(Class clazz, Serializable id, LockMode lockMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(Class clazz, Serializable id, LockOptions lockOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(String entityName, Serializable id) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public Object get(String entityName, Serializable id, LockMode lockMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(String entityName, Serializable id, LockOptions lockOptions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getEntityName(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdentifierLoadAccess byId(String entityName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdentifierLoadAccess byId(Class entityClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NaturalIdLoadAccess byNaturalId(String entityName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NaturalIdLoadAccess byNaturalId(Class entityClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleNaturalIdLoadAccess bySimpleNaturalId(String entityName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleNaturalIdLoadAccess bySimpleNaturalId(Class entityClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Filter enableFilter(String filterName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Filter getEnabledFilter(String filterName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disableFilter(String filterName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SessionStatistics getStatistics() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReadOnly(Object entityOrProxy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setReadOnly(Object entityOrProxy, boolean readOnly) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void doWork(Work work) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T doReturningWork(ReturningWork<T> work) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection disconnect() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reconnect(Connection connection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void enableFetchProfile(String name) throws UnknownProfileException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disableFetchProfile(String name) throws UnknownProfileException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TypeHelper getTypeHelper() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LobHelper getLobHelper() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTenantIdentifier() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Transaction beginTransaction() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Transaction getTransaction() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Query getNamedQuery(String queryName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Query createQuery(String queryString) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLQuery createSQLQuery(String queryString) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria createCriteria(Class persistentClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria createCriteria(Class persistentClass, String alias) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria createCriteria(String entityName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Criteria createCriteria(String entityName, String alias) {
        throw new UnsupportedOperationException();
    }
}

/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.entities.mapper.relation.lazy;

import org.hibernate.engine.*;
import org.hibernate.engine.query.sql.NativeSQLQuerySpecification;
import org.hibernate.*;
import org.hibernate.event.EventListeners;
import org.hibernate.type.Type;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.impl.CriteriaImpl;
import org.hibernate.jdbc.Batcher;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.collection.PersistentCollection;

import java.io.Serializable;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.sql.Connection;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractDelegateSessionImplementor implements SessionImplementor {
    private SessionImplementor delegate;

    public AbstractDelegateSessionImplementor(SessionImplementor delegate) {
        this.delegate = delegate;
    }

    public abstract Object doImmediateLoad(String entityName);

    public Object immediateLoad(String entityName, Serializable id) throws HibernateException {
        return doImmediateLoad(entityName);
    }

    // Delegate methods

	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return delegate.getLoadQueryInfluencers();
	}

	public Interceptor getInterceptor() {
        return delegate.getInterceptor();
    }

    public void setAutoClear(boolean enabled) {
        delegate.setAutoClear(enabled);
    }

    public boolean isTransactionInProgress() {
        return delegate.isTransactionInProgress();
    }

    public void initializeCollection(PersistentCollection collection, boolean writing) throws HibernateException {
        delegate.initializeCollection(collection, writing);
    }

    public Object internalLoad(String entityName, Serializable id, boolean eager, boolean nullable) throws HibernateException {
        return delegate.internalLoad(entityName, id, eager, nullable);
    }

    public long getTimestamp() {
        return delegate.getTimestamp();
    }

    public SessionFactoryImplementor getFactory() {
        return delegate.getFactory();
    }

    public Batcher getBatcher() {
        return delegate.getBatcher();
    }

    public List list(String query, QueryParameters queryParameters) throws HibernateException {
        return delegate.list(query, queryParameters);
    }

    public Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException {
        return delegate.iterate(query, queryParameters);
    }

    public ScrollableResults scroll(String query, QueryParameters queryParameters) throws HibernateException {
        return delegate.scroll(query, queryParameters);
    }

    public ScrollableResults scroll(CriteriaImpl criteria, ScrollMode scrollMode) {
        return delegate.scroll(criteria, scrollMode);
    }

    public List list(CriteriaImpl criteria) {
        return delegate.list(criteria);
    }

    public List listFilter(Object collection, String filter, QueryParameters queryParameters) throws HibernateException {
        return delegate.listFilter(collection, filter, queryParameters);
    }

    public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters) throws HibernateException {
        return delegate.iterateFilter(collection, filter, queryParameters);
    }

    public EntityPersister getEntityPersister(String entityName, Object object) throws HibernateException {
        return delegate.getEntityPersister(entityName, object);
    }

    public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException {
        return delegate.getEntityUsingInterceptor(key);
    }

    public void afterTransactionCompletion(boolean successful, Transaction tx) {
        delegate.afterTransactionCompletion(successful, tx);
    }

    public void beforeTransactionCompletion(Transaction tx) {
        delegate.beforeTransactionCompletion(tx);
    }

    public Serializable getContextEntityIdentifier(Object object) {
        return delegate.getContextEntityIdentifier(object);
    }

    public String bestGuessEntityName(Object object) {
        return delegate.bestGuessEntityName(object);
    }

    public String guessEntityName(Object entity) throws HibernateException {
        return delegate.guessEntityName(entity);
    }

    public Object instantiate(String entityName, Serializable id) throws HibernateException {
        return delegate.instantiate(entityName, id);
    }

    public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) throws HibernateException {
        return delegate.listCustomQuery(customQuery, queryParameters);
    }

    public ScrollableResults scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) throws HibernateException {
        return delegate.scrollCustomQuery(customQuery, queryParameters);
    }

    public List list(NativeSQLQuerySpecification spec, QueryParameters queryParameters) throws HibernateException {
        return delegate.list(spec, queryParameters);
    }

    public ScrollableResults scroll(NativeSQLQuerySpecification spec, QueryParameters queryParameters) throws HibernateException {
        return delegate.scroll(spec, queryParameters);
    }

    public Object getFilterParameterValue(String filterParameterName) {
        return delegate.getFilterParameterValue(filterParameterName);
    }

    public Type getFilterParameterType(String filterParameterName) {
        return delegate.getFilterParameterType(filterParameterName);
    }

    public Map getEnabledFilters() {
        return delegate.getEnabledFilters();
    }

    public int getDontFlushFromFind() {
        return delegate.getDontFlushFromFind();
    }

    public EventListeners getListeners() {
        return delegate.getListeners();
    }

    public PersistenceContext getPersistenceContext() {
        return delegate.getPersistenceContext();
    }

    public int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException {
        return delegate.executeUpdate(query, queryParameters);
    }

    public int executeNativeUpdate(NativeSQLQuerySpecification specification, QueryParameters queryParameters) throws HibernateException {
        return delegate.executeNativeUpdate(specification, queryParameters);
    }

    public EntityMode getEntityMode() {
        return delegate.getEntityMode();
    }

    public CacheMode getCacheMode() {
        return delegate.getCacheMode();
    }

    public void setCacheMode(CacheMode cm) {
        delegate.setCacheMode(cm);
    }

    public boolean isOpen() {
        return delegate.isOpen();
    }

    public boolean isConnected() {
        return delegate.isConnected();
    }

    public FlushMode getFlushMode() {
        return delegate.getFlushMode();
    }

    public void setFlushMode(FlushMode fm) {
        delegate.setFlushMode(fm);
    }

    public Connection connection() {
        return delegate.connection();
    }

    public void flush() {
        delegate.flush();
    }

    public Query getNamedQuery(String name) {
        return delegate.getNamedQuery(name);
    }

    public Query getNamedSQLQuery(String name) {
        return delegate.getNamedSQLQuery(name);
    }

    public boolean isEventSource() {
        return delegate.isEventSource();
    }

    public void afterScrollOperation() {
        delegate.afterScrollOperation();
    }

    public void setFetchProfile(String name) {
        delegate.setFetchProfile(name);
    }

    public String getFetchProfile() {
        return delegate.getFetchProfile();
    }

    public JDBCContext getJDBCContext() {
        return delegate.getJDBCContext();
    }

    public boolean isClosed() {
        return delegate.isClosed();
    }
}

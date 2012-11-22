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

package org.hibernate.shards.util;

import org.hibernate.CallbackException;
import org.hibernate.EntityMode;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Iterator;

/**
 * {@link Interceptor} implementation that delegates all calls to an inner
 * {@link Interceptor}.
 *
 * @author maxr@google.com (Max Ross)
 */
public class InterceptorDecorator implements Interceptor {

  protected final Interceptor delegate;

  public InterceptorDecorator(Interceptor delegate) {
    this.delegate = delegate;
  }

  public boolean onLoad(Object entity, Serializable id, Object[] state,
      String[] propertyNames, Type[] types) throws CallbackException {
    return delegate.onLoad(entity, id, state, propertyNames, types);
  }

  public boolean onFlushDirty(Object entity, Serializable id,
      Object[] currentState, Object[] previousState, String[] propertyNames,
      Type[] types) throws CallbackException {
    return delegate.onFlushDirty(entity, id, currentState, previousState,
        propertyNames, types);
  }

  public boolean onSave(Object entity, Serializable id, Object[] state,
      String[] propertyNames, Type[] types) throws CallbackException {
    return delegate.onSave(entity, id, state, propertyNames, types);
  }

  public void onDelete(Object entity, Serializable id, Object[] state,
      String[] propertyNames, Type[] types) throws CallbackException {
    delegate.onDelete(entity, id, state, propertyNames, types);
  }

  public void onCollectionRecreate(Object collection, Serializable key) throws
      CallbackException {
    delegate.onCollectionRecreate(collection, key);
  }

  public void onCollectionRemove(Object collection, Serializable key) throws
      CallbackException {
    delegate.onCollectionRemove(collection, key);
  }

  public void onCollectionUpdate(Object collection, Serializable key) throws
      CallbackException {
    delegate.onCollectionUpdate(collection, key);
  }

  public void preFlush(Iterator entities) throws CallbackException {
    delegate.preFlush(entities);
  }

  public void postFlush(Iterator entities) throws CallbackException {
    delegate.postFlush(entities);
  }

  public Boolean isTransient(Object entity) {
    return delegate.isTransient(entity);
  }

  public int[] findDirty(Object entity, Serializable id, Object[] currentState,
      Object[] previousState, String[] propertyNames, Type[] types) {
    return delegate
        .findDirty(entity, id, currentState, previousState, propertyNames, types);
  }

  public Object instantiate(String entityName, EntityMode entityMode,
      Serializable id) throws CallbackException {
    return delegate.instantiate(entityName, entityMode, id);
  }

  public String getEntityName(Object object) throws CallbackException {
    return delegate.getEntityName(object);
  }

  public Object getEntity(String entityName, Serializable id) throws
      CallbackException {
    return delegate.getEntity(entityName, id);
  }

  public void afterTransactionBegin(Transaction tx) {
    delegate.afterTransactionBegin(tx);
  }

  public void beforeTransactionCompletion(Transaction tx) {
    delegate.beforeTransactionCompletion(tx);
  }

  public void afterTransactionCompletion(Transaction tx) {
    delegate.afterTransactionCompletion(tx);
  }

  public String onPrepareStatement(String sql) {
    return delegate.onPrepareStatement(sql);
  }

  public Interceptor getDelegate() {
    return delegate;
  }
}

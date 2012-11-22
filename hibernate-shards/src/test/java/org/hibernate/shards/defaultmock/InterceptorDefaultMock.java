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

import org.hibernate.CallbackException;
import org.hibernate.EntityMode;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Iterator;

/**
 * @author maxr@google.com (Max Ross)
 */
public class InterceptorDefaultMock implements Interceptor {

  public boolean onLoad(Object entity, Serializable id, Object[] state,
      String[] propertyNames, Type[] types) throws CallbackException {
    throw new UnsupportedOperationException();
  }

  public boolean onFlushDirty(Object entity, Serializable id,
      Object[] currentState, Object[] previousState, String[] propertyNames,
      Type[] types) throws CallbackException {
    throw new UnsupportedOperationException();
  }

  public boolean onSave(Object entity, Serializable id, Object[] state,
      String[] propertyNames, Type[] types) throws CallbackException {
    throw new UnsupportedOperationException();
  }

  public void onDelete(Object entity, Serializable id, Object[] state,
      String[] propertyNames, Type[] types) throws CallbackException {
    throw new UnsupportedOperationException();
  }

  public void onCollectionRecreate(Object collection, Serializable key)
      throws CallbackException {
    throw new UnsupportedOperationException();
  }

  public void onCollectionRemove(Object collection, Serializable key)
      throws CallbackException {
    throw new UnsupportedOperationException();
  }

  public void onCollectionUpdate(Object collection, Serializable key)
      throws CallbackException {
    throw new UnsupportedOperationException();
  }

  public void preFlush(Iterator entities) throws CallbackException {
    throw new UnsupportedOperationException();
  }

  public void postFlush(Iterator entities) throws CallbackException {
    throw new UnsupportedOperationException();
  }

  public Boolean isTransient(Object entity) {
    throw new UnsupportedOperationException();
  }

  public int[] findDirty(Object entity, Serializable id, Object[] currentState,
      Object[] previousState, String[] propertyNames, Type[] types) {
    throw new UnsupportedOperationException();
  }

  public Object instantiate(String entityName, EntityMode entityMode,
      Serializable id) throws CallbackException {
    throw new UnsupportedOperationException();
  }

  public String getEntityName(Object object) throws CallbackException {
    throw new UnsupportedOperationException();
  }

  public Object getEntity(String entityName, Serializable id)
      throws CallbackException {
    throw new UnsupportedOperationException();
  }

  public void afterTransactionBegin(Transaction tx) {
    throw new UnsupportedOperationException();
  }

  public void beforeTransactionCompletion(Transaction tx) {
    throw new UnsupportedOperationException();
  }

  public void afterTransactionCompletion(Transaction tx) {
    throw new UnsupportedOperationException();
  }

  public String onPrepareStatement(String sql) {
    throw new UnsupportedOperationException();
  }
}

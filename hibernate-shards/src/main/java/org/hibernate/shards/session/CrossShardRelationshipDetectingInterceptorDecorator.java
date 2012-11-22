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

package org.hibernate.shards.session;

import org.hibernate.CallbackException;
import org.hibernate.Interceptor;
import org.hibernate.shards.util.InterceptorDecorator;
import org.hibernate.type.Type;

import java.io.Serializable;

/**
 * Decorator that checks for cross shard relationships before delegating
 * to the decorated interceptor.
 *
 * @author maxr@google.com (Max Ross)
 */
class CrossShardRelationshipDetectingInterceptorDecorator extends InterceptorDecorator {

  private final CrossShardRelationshipDetectingInterceptor csrdi;

  public CrossShardRelationshipDetectingInterceptorDecorator(
      CrossShardRelationshipDetectingInterceptor csrdi,
      Interceptor delegate) {
    super(delegate);
    this.csrdi = csrdi;
  }

  @Override
  public boolean onFlushDirty(Object entity, Serializable id,
      Object[] currentState, Object[] previousState, String[] propertyNames,
      Type[] types) throws CallbackException {

    // first give the cross relationship detector a chance
    csrdi.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
    // now pass it on
    return
        delegate.onFlushDirty(
            entity,
            id,
            currentState,
            previousState,
            propertyNames,
            types);
  }

  @Override
  public void onCollectionUpdate(Object collection, Serializable key)
      throws CallbackException {
    // first give the cross relationship detector a chance
    csrdi.onCollectionUpdate(collection, key);
    // now pass it on
    delegate.onCollectionUpdate(collection, key);
  }

  CrossShardRelationshipDetectingInterceptor getCrossShardRelationshipDetectingInterceptor() {
    return csrdi;
  }
}

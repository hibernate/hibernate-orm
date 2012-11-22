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

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.shards.ShardId;
import org.hibernate.shards.strategy.ShardStrategyFactory;

import java.util.List;

/**
 * Shard-aware extension to {@link SessionFactory}.  Similar to {@link SessionFactory},
 * ShardedSessionFactory is threadsafe.
 *
 * @author maxr@google.com (Max Ross)
 */
public interface ShardedSessionFactory extends SessionFactory {

  /**
   * @return All an unmodifiable list of the {@link SessionFactory} objects contained within.
   */
  List<SessionFactory> getSessionFactories();

  /**
   * This method is provided to allow a client to work on a subset of
   * shards or a specialized {@link ShardStrategyFactory}.  By providing
   * the desired shardIds, the client can limit operations to these shards.
   * Alternatively, this method can be used to create a ShardedSessionFactory
   * with different strategies that might be appropriate for a specific operation.
   *
   * The factory returned will not be stored as one of the factories that would
   * be returned by a call to getSessionFactories.
   *
   * @param shardIds
   * @param shardStrategyFactory
   * @return specially configured ShardedSessionFactory
   */
  ShardedSessionFactory getSessionFactory(List<ShardId> shardIds,
      ShardStrategyFactory shardStrategyFactory);

/**
 * Create database connection(s) and open a <tt>ShardedSession</tt> on it,
 * specifying an interceptor.
 *
 * @param interceptor a session-scoped interceptor
 * @return ShardedSession
 * @throws org.hibernate.HibernateException
 */
 ShardedSession openSession(Interceptor interceptor) throws HibernateException;

  /**
   * Create database connection(s) and open a <tt>ShardedSession</tt> on it.
   *
   * @return ShardedSession
   * @throws org.hibernate.HibernateException
   */
  public ShardedSession openSession() throws HibernateException;
}

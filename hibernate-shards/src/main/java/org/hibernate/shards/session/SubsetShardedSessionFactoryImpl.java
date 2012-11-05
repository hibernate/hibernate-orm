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
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.shards.ShardId;
import org.hibernate.shards.strategy.ShardStrategyFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class extends ShardedSessionFactoryImpl and is constructed by supplying
 * a subset of shardIds that are primarily owned by a ShardedSessionFactoryImpl.
 * The purpose of this class is to override the .close() method in order to
 * prevent the SubsetShardedSessionFactoryImpl from closing any session
 * factories that belong to a ShardedSessionFactoryImpl.
 *
 * @author Maulik Shah@google.com (Maulik Shah)
 */
public class SubsetShardedSessionFactoryImpl extends ShardedSessionFactoryImpl {

  public SubsetShardedSessionFactoryImpl(List<ShardId> shardIds,
      Map<SessionFactoryImplementor, Set<ShardId>> sessionFactoryShardIdMap,
      ShardStrategyFactory shardStrategyFactory,
      Set<Class<?>> classesWithoutTopLevelSaveSupport,
      boolean checkAllAssociatedObjectsForDifferentShards) {
    super(shardIds, sessionFactoryShardIdMap, shardStrategyFactory,
        classesWithoutTopLevelSaveSupport,
        checkAllAssociatedObjectsForDifferentShards);
  }

  protected SubsetShardedSessionFactoryImpl(
      Map<SessionFactoryImplementor, Set<ShardId>> sessionFactoryShardIdMap,
      ShardStrategyFactory shardStrategyFactory,
      Set<Class<?>> classesWithoutTopLevelSaveSupport,
      boolean checkAllAssociatedObjectsForDifferentShards) {
    super(sessionFactoryShardIdMap, shardStrategyFactory,
        classesWithoutTopLevelSaveSupport,
        checkAllAssociatedObjectsForDifferentShards);
  }

  /**
   * This method is a NO-OP. As a ShardedSessionFactoryImpl that represents
   * a subset of the application's shards, it will not close any shard's
   * sessionFactory.
   *
   * @throws HibernateException
   */
  @Override
  public void close() throws HibernateException {
    // no-op: this class should never close session factories
  }

}

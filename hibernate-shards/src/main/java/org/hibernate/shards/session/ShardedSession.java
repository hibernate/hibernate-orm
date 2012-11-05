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

import org.hibernate.classic.Session;
import org.hibernate.shards.ShardId;


/**
 * The main runtime inteface between Java application and Hibernate Shards.<br>
 * ShardedSession represents a logical transaction that might be spanning
 * multiple shards. It follows the contract set by Session API, and adds some
 * shard-related methods.
 *
 * @see ShardedSessionFactory
 * @see Session
 * @author maxr@google.com (Max Ross)
 */
public interface ShardedSession extends Session {

  /**
   * Gets the non-sharded session with which the objects is associated.
   *
   * @param obj  the object for which we want the Session
   * @return the Session with which this object is associated, or null if the
   * object is not associated with a session belonging to this ShardedSession
   */
  Session getSessionForObject(Object obj);

  /**
   * Gets the ShardId of the shard with which the objects is associated.
   *
   * @param obj  the object for which we want the Session
   * @return the ShardId of the Shard with which this object is associated, or
   * null if the object is not associated with a shard belonging to this
   * ShardedSession
   */
  ShardId getShardIdForObject(Object obj);

  /**
   * Place the session into a state where every create operation takes place
   * on the same shard.  Once the shard is locked on a session it cannot
   * be unlocked.
   */
  void lockShard();
}

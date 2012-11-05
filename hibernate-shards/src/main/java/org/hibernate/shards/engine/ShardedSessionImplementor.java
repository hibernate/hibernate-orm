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

package org.hibernate.shards.engine;

import org.hibernate.shards.Shard;
import org.hibernate.shards.session.ShardedSession;

import java.util.List;

/**
 * Defines the internal contract between the <tt>ShardedSession</tt> and other
 * parts of Hibernate Shards.
 *
 * @see ShardedSession the interface to the application
 * @see org.hibernate.shards.session.ShardedSessionImpl the actual implementation
 *
 * @author Tomislav Nad
 */
public interface ShardedSessionImplementor {

  /**
   * Gets all the shards the ShardedSession is spanning.
   *
   * @return list of all shards the ShardedSession is associated with
   */
  List<Shard> getShards();

}

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

package org.hibernate.shards.cfg;

import org.hibernate.cfg.Environment;

/**
 * Hibernate Shards configuration properties.
 * @see Environment
 *
 * @author maxr@google.com (Max Ross)
 */
public final class ShardedEnvironment {

  /**
   * Configuration property that determines whether or not we examine all
   * associated objects for shard conflicts when we save or update.  A shard
   * conflict is when we attempt to associate one object that lives on shard X
   * with an object that lives on shard Y.  Turning this on will hurt
   * performance but will prevent the programmer from ending up with the
   * same entity on multiple shards, which is bad (at least in the current version). 
   */
  public static final String CHECK_ALL_ASSOCIATED_OBJECTS_FOR_DIFFERENT_SHARDS = "hibernate.shard.enable_cross_shard_relationship_checks";

  /**
   * Unique identifier for a shard.  Must be an Integer.
   */
  public static final String SHARD_ID_PROPERTY = "hibernate.connection.shard_id";

  private ShardedEnvironment() {}
}

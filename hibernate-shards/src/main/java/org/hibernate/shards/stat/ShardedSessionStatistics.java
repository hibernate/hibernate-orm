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

package org.hibernate.shards.stat;

import org.hibernate.Session;
import org.hibernate.engine.CollectionKey;
import org.hibernate.engine.EntityKey;
import org.hibernate.shards.Shard;
import org.hibernate.shards.engine.ShardedSessionImplementor;
import org.hibernate.shards.session.OpenSessionEvent;
import org.hibernate.shards.util.Sets;
import org.hibernate.stat.SessionStatistics;

import java.util.Set;

/**
 * Sharded implementation of the SessionStatistics that aggregates the
 * statistics of all underlying individual SessionStatistics.
 *
 * @author Tomislav Nad
 */
public class ShardedSessionStatistics implements SessionStatistics {

  private final Set<SessionStatistics> sessionStatistics;

  public ShardedSessionStatistics(ShardedSessionImplementor session) {
    sessionStatistics = Sets.newHashSet();
    for (Shard s : session.getShards()) {
      if (s.getSession() != null) {
        sessionStatistics.add(s.getSession().getStatistics());
      } else {
        OpenSessionEvent ose = new OpenSessionEvent() {
          public void onOpenSession(Session session) {
            sessionStatistics.add(session.getStatistics());
          }
        };
        s.addOpenSessionEvent(ose);
      }
    }
  }

  public int getEntityCount() {
    int count = 0;
    for (SessionStatistics s : sessionStatistics) {
      count += s.getEntityCount();
    }
    return count;
  }

  public int getCollectionCount() {
    int count = 0;
    for (SessionStatistics s : sessionStatistics) {
      count += s.getCollectionCount();
    }
    return count;
  }

  public Set<EntityKey> getEntityKeys() {
    Set<EntityKey> entityKeys = Sets.newHashSet();
    for (SessionStatistics s : sessionStatistics) {
      @SuppressWarnings("unchecked")
      Set<EntityKey> shardEntityKeys = (Set<EntityKey>)s.getEntityKeys();
      entityKeys.addAll(shardEntityKeys);
    }
    return entityKeys;
  }

  public Set<CollectionKey> getCollectionKeys() {
    Set<CollectionKey> collectionKeys = Sets.newHashSet();
    for (SessionStatistics s : sessionStatistics) {
      @SuppressWarnings("unchecked")
      Set<CollectionKey> shardCollectionKeys = (Set<CollectionKey>)s.getCollectionKeys();
      collectionKeys.addAll(shardCollectionKeys);
    }
    return collectionKeys;
  }
}

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

package org.hibernate.shards;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.shards.defaultmock.SessionFactoryDefaultMock;
import org.hibernate.shards.engine.ShardedSessionFactoryImplementor;
import org.hibernate.shards.session.ShardedSession;
import org.hibernate.shards.session.ShardedSessionFactory;
import org.hibernate.shards.strategy.ShardStrategyFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author maxr@google.com (Max Ross)
 */
public class ShardedSessionFactoryDefaultMock extends SessionFactoryDefaultMock implements ShardedSessionFactoryImplementor {

    @Override
    public Map<SessionFactoryImplementor, Set<ShardId>> getSessionFactoryShardIdMap() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsFactory(SessionFactoryImplementor factory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SessionFactory> getSessionFactories() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ShardedSessionFactory getSessionFactory(List<ShardId> shardIds, ShardStrategyFactory shardStrategyFactory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ShardedSession openSession(Interceptor interceptor) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ShardedSession openSession() throws HibernateException {
        throw new UnsupportedOperationException();
    }
}

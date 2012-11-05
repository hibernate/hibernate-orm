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

package org.hibernate.shards.integration.model;

import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.TransactionException;
import org.hibernate.impl.SessionImpl;
import org.hibernate.shards.integration.BaseShardingIntegrationTestCase;
import org.hibernate.shards.model.Building;
import org.hibernate.shards.session.BaseStatefulInterceptorFactory;
import org.hibernate.shards.session.RequiresSession;
import org.hibernate.shards.session.ShardedSession;
import org.hibernate.shards.util.Lists;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.List;

/**
 * @author maxr@google.com (Max Ross)
 */
public class InterceptorBehaviorPermutedIntegrationTest extends BaseShardingIntegrationTestCase {

  private Interceptor interceptor;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    interceptor = new ExplosiveUpdateInterceptor();
  }

  @Override
  protected ShardedSession openSession() {
    return sf.openSession(interceptor);
  }

  public void testInterceptorBehaviorOnCommit() {
    session.beginTransaction();
    Building b = ModelDataFactory.building("b");
    session.save(b);
    session.getTransaction().commit();
    resetSession();
    session.beginTransaction();
    b = reload(b);
    b.setName("updated b");
    try {
      session.getTransaction().commit();
      fail("expected TransactionException");
    } catch (TransactionException te) {
      // good
    }
    resetSession();
    b = reload(b);
    assertEquals("b", b.getName());
  }

  public void testInterceptorBehaviorOnFlush() {
    session.beginTransaction();
    Building b = ModelDataFactory.building("b");
    session.save(b);
    session.getTransaction().commit();
    resetSession();
    session.beginTransaction();
    b = reload(b);
    b.setName("updated b");
    try {
      session.flush();
      fail("expected CallbackException");
    } catch (CallbackException ce) {
      // good
    }
    resetSession();
    b = reload(b);
    assertEquals("b", b.getName());
  }

  public void testStatefulInterceptorBehavior() {
    final int[] calls = {0};
    interceptor = new BaseStatefulInterceptorFactory() {
      public Interceptor newInstance() {
        calls[0]++;
        return new ExplosiveUpdateInterceptor();
      }
    };
    resetSession();

    // this is how we know we were getting different interceptors
    // for each shard
    assertEquals(getNumDatabases(), calls[0]);
  }

  public void testStatefulInterceptorWithRequiresSessionBehavior() {
    final List<Interceptor> interceptors = Lists.newArrayList();
    class MyInterceptor extends EmptyInterceptor implements RequiresSession {
      private boolean[] wasCalled = {false};
      public void setSession(Session session) {
        assertTrue(session instanceof SessionImpl);
        wasCalled[0] = true;
      }
    }

    final int[] calls = {0};
    interceptor = new BaseStatefulInterceptorFactory() {
      public Interceptor newInstance() {
        calls[0]++;
        Interceptor interceptor = new MyInterceptor();
        interceptors.add(interceptor);
        return interceptor;
      }
    };
    resetSession();
    session.createCriteria(Building.class).list(); // force the session to init
    // this is how we know we were getting different interceptors
    // for each shard
    assertEquals(getNumDatabases(), calls[0]);
    for(Interceptor interceptor : interceptors) {
      assertTrue(((MyInterceptor)interceptor).wasCalled[0]);
    }

  }

  private static final class ExplosiveUpdateInterceptor extends EmptyInterceptor {

    @Override
    public boolean onFlushDirty(Object entity, Serializable id,
        Object[] currentState, Object[] previousState, String[] propertyNames,
        Type[] types) {
      throw new CallbackException("boom");
    }
  }
}

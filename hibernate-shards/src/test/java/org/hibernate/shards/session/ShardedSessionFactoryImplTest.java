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

import junit.framework.TestCase;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Settings;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.shards.ShardId;
import org.hibernate.shards.defaultmock.SessionFactoryDefaultMock;
import org.hibernate.shards.strategy.ShardStrategy;
import org.hibernate.shards.strategy.ShardStrategyDefaultMock;
import org.hibernate.shards.strategy.ShardStrategyFactory;
import org.hibernate.shards.strategy.ShardStrategyFactoryDefaultMock;
import org.hibernate.shards.util.Maps;
import org.hibernate.shards.util.Sets;

import javax.naming.NamingException;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author maxr@google.com (Max Ross)
 */
public class ShardedSessionFactoryImplTest extends TestCase {
  private SessionFactoryImplementor sf;
  private ShardId shardId;

  @Override
  protected void setUp() {
    sf = new SessionFactoryDefaultMock() {
      @Override
      public Settings getSettings() {
        Configuration config = new Configuration();
        Properties prop = new Properties();
        prop.setProperty(Environment.SESSION_FACTORY_NAME, "1");
        prop.setProperty(Environment.DIALECT, "org.hibernate.dialect.MySQLInnoDBDialect");
        return config.buildSettings(prop);
      }

      @Override
      public void close() throws HibernateException {
      }

      @Override
      public Map getAllClassMetadata() throws HibernateException {
        return Collections.emptyMap();
      }
    };
    shardId = new ShardId(1);
  }

  private ShardStrategyFactory buildStrategyFactoryDefaultMock() {
    return new ShardStrategyFactoryDefaultMock() {
      @Override
      public ShardStrategy newShardStrategy(List<ShardId> shardIds) {
        return new ShardStrategyDefaultMock();
      }
    };
  }

  public void testCtors() {
    Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
    Set<Class<?>> crsl = Collections.emptySet();
    ShardStrategyFactory shardStrategyFactory = buildStrategyFactoryDefaultMock();
    try {
      new ShardedSessionFactoryImpl(null, shardStrategyFactory, crsl, false);
      fail("expected npe");
    } catch (NullPointerException npe) {
      // good
    }

    try {
      new ShardedSessionFactoryImpl(sfMap, null, crsl, false);
      fail("expected iae");
    } catch (IllegalArgumentException iae) {
      // good
    }

    sfMap.put(this.sf, Sets.newHashSet(shardId));
    try {
      new ShardedSessionFactoryImpl(sfMap, null, crsl, false);
      fail("expected npe");
    } catch (NullPointerException npe) {
      // good
    }

    try {
      new ShardedSessionFactoryImpl(sfMap, shardStrategyFactory, null, false);
      fail("expected npe");
    } catch (NullPointerException npe) {
      // good
    }

    ShardedSessionFactoryImpl ssfi = new ShardedSessionFactoryImpl(sfMap, shardStrategyFactory, crsl, false);
    ssfi.close();
  }

  public void testOpenSessionWithUserSuppliedConnection() {
    ShardStrategyFactory shardStrategyFactory = buildStrategyFactoryDefaultMock();
    Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
    sfMap.put(sf, Sets.newHashSet(shardId));

    ShardedSessionFactory ssf =
        new ShardedSessionFactoryImpl(
            sfMap,
            shardStrategyFactory,
            Collections.<Class<?>>emptySet(),
            false);
    try {
      Connection conn = null;
      try {
        ssf.openSession(conn);
        fail("Expected uoe");
      } catch (UnsupportedOperationException uoe) {
        // good
      }
      Interceptor interceptor = null;
      try {
        ssf.openSession(conn, interceptor);
        fail("Expected uoe");
      } catch (UnsupportedOperationException uoe) {
        // good
      }
    } finally {
      ssf.close();
    }
  }

  public void testOpenStatelessSessionWithUserSuppliedConnection() {
    ShardStrategyFactory shardStrategyFactory = buildStrategyFactoryDefaultMock();
    Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
    sfMap.put(sf, Sets.newHashSet(shardId));

    ShardedSessionFactory ssf =
        new ShardedSessionFactoryImpl(
            sfMap,
            shardStrategyFactory,
            Collections.<Class<?>>emptySet(),
            false);
    try {
      Connection conn = null;
      try {
        ssf.openStatelessSession(conn);
        fail("Expected uoe");
      } catch (UnsupportedOperationException uoe) {
        // good
      }
    } finally {
      ssf.close();
    }
  }

  public void testIsClosed() {
    ShardStrategyFactory shardStrategyFactory = buildStrategyFactoryDefaultMock();

    SessionFactoryDefaultMock mock1 = new SessionFactoryDefaultMock() {

      @Override
      public boolean isClosed() {
        return false;
      }

      @Override
      public Settings getSettings() {
        Configuration config = new Configuration();
        Properties prop = new Properties();
        prop.setProperty(Environment.SESSION_FACTORY_NAME, "1");
        prop.setProperty(Environment.DIALECT, "org.hibernate.dialect.MySQLInnoDBDialect");
        return config.buildSettings(prop);
      }

      @Override
      public void close() throws HibernateException {
      }

      @Override
      public Map getAllClassMetadata() throws HibernateException {
        return Collections.emptyMap();
      }
    };
    Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
    sfMap.put(mock1, Sets.newHashSet(new ShardId(1)));
    ShardedSessionFactory ssf =
        new ShardedSessionFactoryImpl(
            sfMap,
            shardStrategyFactory,
            Collections.<Class<?>>emptySet(),
            false);
    try {
      assertFalse(ssf.isClosed());
    } finally {
      ssf.close();
    }
    SessionFactoryDefaultMock mock2 = new SessionFactoryDefaultMock() {

      @Override
      public boolean isClosed() {
        return true;
      }

      @Override
      public Settings getSettings() {
        Configuration config = new Configuration();
        Properties prop = new Properties();
        prop.setProperty(Environment.SESSION_FACTORY_NAME, "2");
        prop.setProperty(Environment.DIALECT, "org.hibernate.dialect.MySQLInnoDBDialect");
        return config.buildSettings(prop);
      }

      @Override
      public void close() throws HibernateException {
      }

      @Override
      public Map getAllClassMetadata() throws HibernateException {
        return Collections.emptyMap();
      }
    };
    sfMap.put(mock2, Sets.newHashSet(new ShardId(2)));
    ssf =
        new ShardedSessionFactoryImpl(
            sfMap,
            shardStrategyFactory,
            Collections.<Class<?>>emptySet(),
            false);
    try {
      assertTrue(ssf.isClosed());
    } finally {
      ssf.close();
    }
  }

  public void testGetReference() throws NamingException {
    ShardStrategyFactory shardStrategyFactory = buildStrategyFactoryDefaultMock();
    Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
    sfMap.put(sf, Sets.newHashSet(shardId));

    ShardedSessionFactory ssf =
        new ShardedSessionFactoryImpl(
            sfMap,
            shardStrategyFactory,
            Collections.<Class<?>>emptySet(),
            false);
    try {
      ssf.getReference();
      fail("Expected uoe");
    } catch (UnsupportedOperationException uoe) {
      // good
    } finally {
      ssf.close();
    }
  }

  public void testGetStatistics() {
    ShardStrategyFactory shardStrategyFactory = buildStrategyFactoryDefaultMock();
    Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
    sfMap.put(sf, Sets.newHashSet(shardId));

    ShardedSessionFactory ssf =
        new ShardedSessionFactoryImpl(
            sfMap,
            shardStrategyFactory,
            Collections.<Class<?>>emptySet(),
            false);
    assertNotNull(ssf.getStatistics());
  }

  public void testFinalizeOnOpenSession() throws Throwable {

    final boolean[] closeCalled = {false};

    ShardStrategyFactory shardStrategyFactory = buildStrategyFactoryDefaultMock();
    Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
    sfMap.put(sf, Sets.newHashSet(shardId));

    ShardedSessionFactoryImpl ssf =
        new ShardedSessionFactoryImpl(
            sfMap,
            shardStrategyFactory,
            Collections.<Class<?>>emptySet(),
            false) {
          @Override
          public void close() throws HibernateException {
            closeCalled[0] = true;
            super.close();
          }

          @Override
          public boolean isClosed() {
            return false;
          }
        };
    ssf.finalize();
    assertTrue(closeCalled[0]);
  }

  public void testFinalizeOnClosedSession() throws Throwable {
    final boolean[] closeCalled = {false};

    ShardStrategyFactory shardStrategyFactory = buildStrategyFactoryDefaultMock();
    Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
    sfMap.put(sf, Sets.newHashSet(shardId));
    ShardedSessionFactoryImpl ssf =
        new ShardedSessionFactoryImpl(
            sfMap,
            shardStrategyFactory,
            Collections.<Class<?>>emptySet(),
            false) {
          @Override
          public void close() throws HibernateException {
            closeCalled[0] = true;
            super.close();
          }

          @Override
          public boolean isClosed() {
            return true;
          }
        };
    ssf.finalize();
    assertFalse(closeCalled[0]);
  }

  public void testFailsWhenMultipleSessionFactoriesHaveSameShardId() {
    Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
    Set<Class<?>> crsl = Collections.emptySet();
    ShardStrategyFactory shardStrategyFactory = buildStrategyFactoryDefaultMock();
    sfMap.put(sf, Sets.newHashSet(shardId));

    SessionFactoryImplementor sf2 = new SessionFactoryDefaultMock() {
      @Override
      public Settings getSettings() {
        Configuration config = new Configuration();
        Properties prop = new Properties();
        prop.setProperty(Environment.SESSION_FACTORY_NAME, "1");
        prop.setProperty(Environment.DIALECT, "org.hibernate.dialect.MySQLInnoDBDialect");
        return config.buildSettings(prop);
      }

      @Override
      public void close() throws HibernateException {
      }

      @Override
      public Map getAllClassMetadata() throws HibernateException {
        return Collections.emptyMap();
      }
    };

    sfMap.put(sf2, Sets.newHashSet(new ShardId(1)));

    try {
      new ShardedSessionFactoryImpl(sfMap, shardStrategyFactory, crsl, false);
      fail("expected hibernate exception");
    } catch (HibernateException he) {
      // good
    }
  }
}

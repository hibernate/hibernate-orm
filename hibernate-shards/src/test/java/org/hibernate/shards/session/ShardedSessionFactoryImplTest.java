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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.shards.ShardId;
import org.hibernate.shards.defaultmock.SessionFactoryDefaultMock;
import org.hibernate.shards.strategy.ShardStrategy;
import org.hibernate.shards.strategy.ShardStrategyDefaultMock;
import org.hibernate.shards.strategy.ShardStrategyFactory;
import org.hibernate.shards.strategy.ShardStrategyFactoryDefaultMock;
import org.hibernate.shards.util.Maps;
import org.hibernate.shards.util.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
public class ShardedSessionFactoryImplTest {
    private SessionFactoryImplementor sf;
    private ShardId shardId;

    @Before
    public void setUp() {
        sf = new SessionFactoryDefaultMock() {
            @Override
            public Settings getSettings() {
                Configuration config = new Configuration();
                Properties prop = new Properties();
                prop.setProperty(Environment.SESSION_FACTORY_NAME, "1");
                prop.setProperty(Environment.DIALECT, "org.hibernate.dialect.MySQLInnoDBDialect");
                return config.buildSettings(prop, null);
            }

            @Override
            public void close() throws HibernateException {
            }

            @Override
            public Map<String, ClassMetadata> getAllClassMetadata() throws HibernateException {
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

    @Test
    public void testCtors() {
        Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
        Set<Class<?>> crsl = Collections.emptySet();
        ShardStrategyFactory shardStrategyFactory = buildStrategyFactoryDefaultMock();
        try {
            new ShardedSessionFactoryImpl(null, shardStrategyFactory, crsl, false);
            Assert.fail("expected npe");
        } catch (NullPointerException npe) {
            // good
        }

        try {
            new ShardedSessionFactoryImpl(sfMap, null, crsl, false);
            Assert.fail("expected iae");
        } catch (IllegalArgumentException iae) {
            // good
        }

        sfMap.put(this.sf, Sets.newHashSet(shardId));
        try {
            new ShardedSessionFactoryImpl(sfMap, null, crsl, false);
            Assert.fail("expected npe");
        } catch (NullPointerException npe) {
            // good
        }

        try {
            new ShardedSessionFactoryImpl(sfMap, shardStrategyFactory, null, false);
            Assert.fail("expected npe");
        } catch (NullPointerException npe) {
            // good
        }

        final ShardedSessionFactoryImpl ssfi = new ShardedSessionFactoryImpl(sfMap, shardStrategyFactory, crsl, false);
        ssfi.close();
    }

    @Test
    public void testOpenSessionWithUserSuppliedConnection() {
        final ShardStrategyFactory shardStrategyFactory = buildStrategyFactoryDefaultMock();
        final Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
        sfMap.put(sf, Sets.newHashSet(shardId));

        final ShardedSessionFactory ssf = new ShardedSessionFactoryImpl(sfMap, shardStrategyFactory,
                Collections.<Class<?>>emptySet(), false);

        try {
            try {
                ssf.openSession();
                Assert.fail("Expected uoe");
            } catch (UnsupportedOperationException uoe) {
                // good
            }
            Interceptor interceptor = null;
            try {
                ssf.openSession(interceptor);
                Assert.fail("Expected uoe");
            } catch (UnsupportedOperationException uoe) {
                // good
            }
        } finally {
            ssf.close();
        }
    }

    @Test
    public void testOpenStatelessSessionWithUserSuppliedConnection() {
        final ShardStrategyFactory shardStrategyFactory = buildStrategyFactoryDefaultMock();
        final Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
        sfMap.put(sf, Sets.newHashSet(shardId));

        final ShardedSessionFactory ssf = new ShardedSessionFactoryImpl(
                sfMap,
                shardStrategyFactory,
                Collections.<Class<?>>emptySet(),
                false);

        try {
            Connection conn = null;
            try {
                ssf.openStatelessSession(conn);
                Assert.fail("Expected uoe");
            } catch (UnsupportedOperationException uoe) {
                // good
            }
        } finally {
            ssf.close();
        }
    }

    @Test
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
                return config.buildSettings(prop, null);
            }

            @Override
            public void close() throws HibernateException {
            }

            @Override
            public Map<String, ClassMetadata> getAllClassMetadata() throws HibernateException {
                return Collections.emptyMap();
            }
        };

        final Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
        sfMap.put(mock1, Sets.newHashSet(new ShardId(1)));
        ShardedSessionFactory ssf =
                new ShardedSessionFactoryImpl(
                        sfMap,
                        shardStrategyFactory,
                        Collections.<Class<?>>emptySet(),
                        false);

        try {
            Assert.assertFalse(ssf.isClosed());
        } finally {
            ssf.close();
        }

        final SessionFactoryDefaultMock mock2 = new SessionFactoryDefaultMock() {
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
                return config.buildSettings(prop, null);
            }

            @Override
            public void close() throws HibernateException {
            }

            @Override
            public Map<String, ClassMetadata> getAllClassMetadata() throws HibernateException {
                return Collections.emptyMap();
            }
        };

        sfMap.put(mock2, Sets.newHashSet(new ShardId(2)));
        ssf = new ShardedSessionFactoryImpl(
                sfMap,
                shardStrategyFactory,
                Collections.<Class<?>>emptySet(),
                false);

        try {
            Assert.assertTrue(ssf.isClosed());
        } finally {
            ssf.close();
        }
    }

    @Test
    public void testGetReference() throws NamingException {
        final ShardStrategyFactory shardStrategyFactory = buildStrategyFactoryDefaultMock();
        final Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
        sfMap.put(sf, Sets.newHashSet(shardId));

        final ShardedSessionFactory ssf = new ShardedSessionFactoryImpl(
                sfMap,
                shardStrategyFactory,
                Collections.<Class<?>>emptySet(),
                false);

        try {
            ssf.getReference();
            Assert.fail("Expected uoe");
        } catch (UnsupportedOperationException uoe) {
            // good
        } finally {
            ssf.close();
        }
    }

    @Test
    public void testGetStatistics() {
        final ShardStrategyFactory shardStrategyFactory = buildStrategyFactoryDefaultMock();
        final Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
        sfMap.put(sf, Sets.newHashSet(shardId));

        final ShardedSessionFactory ssf = new ShardedSessionFactoryImpl(
                sfMap,
                shardStrategyFactory,
                Collections.<Class<?>>emptySet(),
                false);

        Assert.assertNotNull(ssf.getStatistics());
    }

    @Test
    public void testFinalizeOnOpenSession() throws Throwable {

        final boolean[] closeCalled = {false};

        final ShardStrategyFactory shardStrategyFactory = buildStrategyFactoryDefaultMock();
        final Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
        sfMap.put(sf, Sets.newHashSet(shardId));

        final ShardedSessionFactoryImpl ssf = new ShardedSessionFactoryImpl(sfMap, shardStrategyFactory,
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
        Assert.assertTrue(closeCalled[0]);
    }

    @Test
    public void testFinalizeOnClosedSession() throws Throwable {
        final boolean[] closeCalled = {false};

        final ShardStrategyFactory shardStrategyFactory = buildStrategyFactoryDefaultMock();
        final Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
        sfMap.put(sf, Sets.newHashSet(shardId));

        final ShardedSessionFactoryImpl ssf = new ShardedSessionFactoryImpl(
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
        Assert.assertFalse(closeCalled[0]);
    }

    @Test
    public void testFailsWhenMultipleSessionFactoriesHaveSameShardId() {
        final Map<SessionFactoryImplementor, Set<ShardId>> sfMap = Maps.newHashMap();
        final Set<Class<?>> crsl = Collections.emptySet();
        final ShardStrategyFactory shardStrategyFactory = buildStrategyFactoryDefaultMock();
        sfMap.put(sf, Sets.newHashSet(shardId));

        final SessionFactoryImplementor sf2 = new SessionFactoryDefaultMock() {
            @Override
            public Settings getSettings() {
                Configuration config = new Configuration();
                Properties prop = new Properties();
                prop.setProperty(Environment.SESSION_FACTORY_NAME, "1");
                prop.setProperty(Environment.DIALECT, "org.hibernate.dialect.MySQLInnoDBDialect");
                return config.buildSettings(prop, null);
            }

            @Override
            public void close() throws HibernateException {
            }

            @Override
            public Map<String, ClassMetadata> getAllClassMetadata() throws HibernateException {
                return Collections.emptyMap();
            }
        };

        sfMap.put(sf2, Sets.newHashSet(new ShardId(1)));

        try {
            new ShardedSessionFactoryImpl(sfMap, shardStrategyFactory, crsl, false);
            Assert.fail("expected hibernate exception");
        } catch (HibernateException he) {
            // good
        }
    }
}

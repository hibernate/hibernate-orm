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

package org.hibernate.shards.integration;

import junit.framework.TestCase;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.shards.ShardId;
import org.hibernate.shards.ShardedConfiguration;
import org.hibernate.shards.cfg.ConfigurationToShardConfigurationAdapter;
import org.hibernate.shards.cfg.ShardConfiguration;
import org.hibernate.shards.integration.platform.DatabasePlatform;
import org.hibernate.shards.integration.platform.DatabasePlatformFactory;
import org.hibernate.shards.loadbalance.RoundRobinShardLoadBalancer;
import org.hibernate.shards.session.ShardedSession;
import org.hibernate.shards.session.ShardedSessionFactory;
import org.hibernate.shards.session.ShardedSessionImpl;
import org.hibernate.shards.strategy.ShardStrategy;
import org.hibernate.shards.strategy.ShardStrategyFactory;
import org.hibernate.shards.strategy.ShardStrategyImpl;
import org.hibernate.shards.strategy.access.ParallelShardAccessStrategy;
import org.hibernate.shards.strategy.access.SequentialShardAccessStrategy;
import org.hibernate.shards.strategy.access.ShardAccessStrategy;
import org.hibernate.shards.strategy.resolution.AllShardsShardResolutionStrategy;
import org.hibernate.shards.strategy.resolution.ShardResolutionStrategy;
import org.hibernate.shards.strategy.selection.RoundRobinShardSelectionStrategy;
import org.hibernate.shards.strategy.selection.ShardSelectionStrategy;
import org.hibernate.shards.util.DatabaseUtils;
import org.hibernate.shards.util.Lists;
import org.hibernate.shards.util.Maps;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Base class for all sharding integration tests.
 * Sets up and tears down in-memory hypersonic dbs.
 * The rest is up to you.
 *
 * @author maxr@google.com (Max Ross)
 */
public abstract class BaseShardingIntegrationTestCase extends TestCase implements HasPermutation {

  private Permutation perm = Permutation.DEFAULT;

  protected ShardedSessionFactory sf;
  protected ShardedSession session;

  public void setPermutation(Permutation perm) {
    this.perm = perm;
  }

  /**
   * Gosh, it sure seems expensive to be initializing a ThreadPoolExecutor
   * for each test that needs it rather than initializing it once and just
   * using it for any test that needs it.  So why do it this way?  Well, first
   * read my novella in MemoryLeakPlugger.  Done?  Ok, welcome back.  So you
   * should now recognize that the MemoryLeakPlugger is used to clear out the
   * value of a specific, problematic ThreadLocal.  But what do you think happens
   * if Hibernate and CGLib were doing their thing in some other thread?
   * Exactly.  The MemoryLeakPlugger isn't going to be able to clear out the
   * Callbacks that were initialized in different threads, and any test
   * that does work in other threads is going to cause memory leaks.  The
   * solution is to make sure the threads die (all ThreadLocals get gc'ed when
   * a Thread dies), so we initialize and shutdown the ThreadPoolExecutor for
   * every test that needs it.
   */
  protected ThreadPoolExecutor executor;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    for(int i = 0; i < getNumDatabases(); i++) {
      DatabaseUtils.destroyDatabase(i, getIdGenType());
      DatabaseUtils.createDatabase(i, getIdGenType());
    }
    Configuration prototypeConfig = buildPrototypeConfig();
    List<ShardConfiguration> configurations = buildConfigurations();
    // now we use these configs to build our sharded config
    ShardStrategyFactory shardStrategyFactory = buildShardStrategyFactory();
    Map<Integer, Integer> virtualShardMap = buildVirtualShardToShardMap();
    // we base the configuration off of the shard0 config with the expectation
    // that all other configs will be the same
    ShardedConfiguration shardedConfig =
        new ShardedConfiguration(
            prototypeConfig,
            configurations,
            shardStrategyFactory,
            virtualShardMap);
    sf = shardedConfig.buildShardedSessionFactory();
    session = openSession();
  }

  protected ShardedSession openSession() {
    return sf.openSession();
  }

  protected Map<Integer, Integer> buildVirtualShardToShardMap() {
    Map<Integer, Integer> virtualShardToShardMap = Maps.newHashMap();
    if (isVirtualShardingEnabled()) {
      for(int i = 0; i < getNumShards(); ++i) {
        virtualShardToShardMap.put(i, i % getNumDatabases());
      }
    }
    return virtualShardToShardMap;
  }

  private Configuration buildPrototypeConfig() {
    DatabasePlatform dbPlatform = DatabasePlatformFactory.FACTORY.getDatabasePlatform();
    String dbPlatformConfigDirectory = "platform/" + dbPlatform.getName().toLowerCase() +"/config/";
    IdGenType idGenType = getIdGenType();
    Configuration config = createPrototypeConfiguration();
    config.configure(BaseShardingIntegrationTestCase.class.getResource(dbPlatformConfigDirectory + "shard0.hibernate.cfg.xml"));
    config.addURL(BaseShardingIntegrationTestCase.class.getResource(dbPlatformConfigDirectory + idGenType.getMappingFile()));
    return config;
  }

  /**
   * You can override this if you want to return your own subclass of Configuration.
   * @return The {@link Configuration} to use as the prototype
   */
  protected Configuration createPrototypeConfiguration() {
    return new Configuration();
  }

  protected List<ShardConfiguration> buildConfigurations() {
    DatabasePlatform dbPlatform = DatabasePlatformFactory.FACTORY.getDatabasePlatform();
    String dbPlatformConfigDirectory = "platform/" + dbPlatform.getName().toLowerCase() +"/config/";
    List<ShardConfiguration> configs = Lists.newArrayList();
    for(int i = 0; i < getNumDatabases(); i++) {
      Configuration config = new Configuration();
      config.configure(BaseShardingIntegrationTestCase.class.getResource(dbPlatformConfigDirectory + "shard" + i + ".hibernate.cfg.xml"));
      configs.add(new ConfigurationToShardConfigurationAdapter(config));
    }
    return configs;
  }

  protected ShardStrategyFactory buildShardStrategyFactory() {
    return new ShardStrategyFactory() {
      public ShardStrategy newShardStrategy(List<ShardId> shardIds) {
        RoundRobinShardLoadBalancer loadBalancer = new RoundRobinShardLoadBalancer(shardIds);
        ShardSelectionStrategy sss = new RoundRobinShardSelectionStrategy(loadBalancer);
        ShardResolutionStrategy srs = new AllShardsShardResolutionStrategy(shardIds);
        ShardAccessStrategy sas = getShardAccessStrategy();
        return new ShardStrategyImpl(sss, srs, sas);
      }
    };
  }

  protected void commitAndResetSession() {
    session.getTransaction().commit();
    resetSession();
    session.beginTransaction();
  }

  protected void resetSession() {
    MemoryLeakPlugger.plug((ShardedSessionImpl)session);
    session.close();
    session = openSession();
  }

  @Override
  protected void tearDown() throws Exception {
    if(executor != null) {
      executor.shutdownNow();
      executor = null;
    }

    try {
      if(session != null) {
        MemoryLeakPlugger.plug((ShardedSessionImpl)session);
        session.close();
        session = null;
      }
    } finally {
      if(sf != null) {
        sf.close();
        sf = null;
      }
    }
    ShardedSessionImpl.setCurrentSubgraphShardId(null);
    super.tearDown();
  }

  /**
   * Override if you want more than the default
   * @return the number of databases
   */
  protected int getNumDatabases() {
    return perm.getNumDbs();
  }

  protected int getNumShards() {
    if (isVirtualShardingEnabled()) {
      return perm.getNumShards();
    }
    return getNumDatabases();
  }

  protected boolean isVirtualShardingEnabled() {
    return perm.isVirtualShardingEnabled();
  }

  protected IdGenType getIdGenType() {
    return perm.getIdGenType();
  }

  protected ShardAccessStrategyType getShardAccessStrategyType() {
    return perm.getSast();
  }

  protected <T> T reloadAssertNotNull(T reloadMe) {
    T result = reload(reloadMe);
    assertNotNull(result);
    return result;
  }

  protected <T> T reload(T reloadMe) {
    return reload(session, reloadMe);
  }

  protected <T> T reloadAssertNotNull(Session session, T reloadMe) {
    T result = reload(session, reloadMe);
    assertNotNull(result);
    return result;
  }

  protected <T> T reload(Session session, T reloadMe) {
    Class<?> clazz = reloadMe.getClass();
    String className = clazz.getSimpleName();
    try {
      Method m = clazz.getMethod("get" + className + "Id");
      @SuppressWarnings("unchecked")
      T result = (T) get(session, clazz, (Serializable) m.invoke(reloadMe));
      return result;
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  protected ShardId getShardIdForObject(Object obj) {
    return session.getShardIdForObject(obj);
  }

  private ShardAccessStrategy getShardAccessStrategy() {
    switch(getShardAccessStrategyType()) {
      case SEQUENTIAL:
        return new SequentialShardAccessStrategy();
      case PARALLEL:
        executor = buildThreadPoolExecutor();
        return new ParallelShardAccessStrategy(executor);
      default:
        throw new RuntimeException("unsupported shard access strategy type");
    }
  }

  private static final ThreadFactory FACTORY = new ThreadFactory() {
    private int nextThreadId = 0;
    public Thread newThread(Runnable r) {
      Thread t = Executors.defaultThreadFactory().newThread(r);
      t.setDaemon(true);
      t.setName("T" + (nextThreadId++));
      return t;
    }
  };

  private ThreadPoolExecutor buildThreadPoolExecutor() {
    return new ThreadPoolExecutor(10, 50, 60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), FACTORY);
  }

  /**
   * Catch all throwables so we get an opportunity to add info about the
   * permutation under which the failure took place. 
   */
  @Override
  public void runBare() throws Throwable {
    try {
      super.runBare();
    } catch (Throwable t) {
      throw new RuntimeException(perm.getMessageWithPermutationPrefix(t.getMessage()), t);
      // TODO(maxr) handel assertion failure separately so they get properly reported
    }
  }

  protected <T> List<T> list(Criteria crit) {
    @SuppressWarnings("unchecked")
    List<T> result = crit.list();
    return result;
  }

  protected <T> List<T> list(Query query) {
    @SuppressWarnings("unchecked")
    List<T> result = query.list();
    return result;
  }

  protected <T> T uniqueResult(Criteria crit) {
    @SuppressWarnings("unchecked")
    T result = (T) crit.uniqueResult();
    return result;
  }

  protected <T> T get(Session session, Class<?> clazz, Serializable id) {
    @SuppressWarnings("unchecked")
    T result = (T) session.get(clazz, id);
    return result;
  }
}



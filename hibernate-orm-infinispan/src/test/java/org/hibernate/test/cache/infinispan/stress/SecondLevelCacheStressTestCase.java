/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.cache.infinispan.stress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.transaction.TransactionManager;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;

import org.hibernate.test.cache.infinispan.stress.entities.Address;
import org.hibernate.test.cache.infinispan.stress.entities.Family;
import org.hibernate.test.cache.infinispan.stress.entities.Person;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.infinispan.util.concurrent.ConcurrentHashSet;

import static org.infinispan.test.TestingUtil.withTx;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Stress test for second level cache.
 *
 * TODO Various:
 * - Switch to a JDBC connection pool to avoid too many connections created
 * (as well as consuming memory, it's expensive to create)
 * - Use barrier associated execution tasks at the beginning and end to track
 * down start/end times for runs.
 *
 * @author Galder Zamarreño
 * @since 4.1
 */
@Ignore
public class SecondLevelCacheStressTestCase {

   static final int NUM_THREADS = 10;
   static final long WARMUP_TIME = TimeUnit.SECONDS.toNanos(Integer.getInteger("warmup-time", 1) * 5);
   static final long RUNNING_TIME = TimeUnit.SECONDS.toNanos(Integer.getInteger("time", 1) * 60);
   static final boolean PROFILE = Boolean.getBoolean("profile");
   static final boolean ALLOCATION = Boolean.getBoolean("allocation");
   static final int RUN_COUNT_LIMIT = Integer.getInteger("count", 1000); // max number of runs per operation
   static final Random RANDOM = new Random(12345);

   String provider;
   ConcurrentHashSet<Integer> updatedIds;
   Queue<Integer> removeIds;
   SessionFactory sessionFactory;
   TransactionManager tm;
   volatile int numEntities;

   @Before
   public void beforeClass() {
      provider = getProvider();

      updatedIds = new ConcurrentHashSet<Integer>();
      removeIds = new ConcurrentLinkedQueue<Integer>();

      StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder().enableAutoClose()
              .applySetting( Environment.USE_SECOND_LEVEL_CACHE, "true" )
              .applySetting( Environment.USE_QUERY_CACHE, "true" )
              .applySetting( Environment.DRIVER, "com.mysql.jdbc.Driver" )
              .applySetting( Environment.URL, "jdbc:mysql://localhost:3306/hibernate" )
              .applySetting( Environment.DIALECT, "org.hibernate.dialect.MySQL5InnoDBDialect" )
              .applySetting( Environment.USER, "root" )
              .applySetting( Environment.PASS, "password" )
              .applySetting( Environment.HBM2DDL_AUTO, "create-drop" );

      // Create database schema in each run
      applyCacheSettings( ssrb );

      StandardServiceRegistry registry = ssrb.build();

      Metadata metadata = buildMetadata( registry );

      sessionFactory = metadata.buildSessionFactory();

      tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
   }

   protected String getProvider() {
      return "infinispan";
   }

   protected void applyCacheSettings(StandardServiceRegistryBuilder ssrb) {
      ssrb.applySetting( Environment.CACHE_REGION_FACTORY, "org.hibernate.cache.infinispan.InfinispanRegionFactory" );
      ssrb.applySetting( Environment.JTA_PLATFORM, "org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform" );
      ssrb.applySetting( InfinispanRegionFactory.INFINISPAN_CONFIG_RESOURCE_PROP, "stress-local-infinispan.xml" );
   }

   @After
   public void afterClass() {
      sessionFactory.close();
   }

   @Test
   public void testEntityLifecycle() throws InterruptedException {
      if (!PROFILE) {
         System.out.printf("[provider=%s] Warming up\n", provider);
         doEntityLifecycle(true);

         // Recreate session factory cleaning everything
         afterClass();
         beforeClass();
      }

      System.out.printf("[provider=%s] Testing...\n", provider);
      doEntityLifecycle(false);
   }

   void doEntityLifecycle(boolean isWarmup) {
      long runningTimeout = isWarmup ? WARMUP_TIME : RUNNING_TIME;
      TotalStats insertPerf = runEntityInsert(runningTimeout);
      numEntities = countEntities().intValue();
      printResult(isWarmup, "[provider=%s] Inserts/s %10.2f (%d entities)\n",
            provider, insertPerf.getOpsPerSec("INSERT"), numEntities);

      TotalStats updatePerf = runEntityUpdate(runningTimeout);
      List<Integer> updateIdsSeq = new ArrayList<Integer>(updatedIds);
      printResult(isWarmup, "[provider=%s] Updates/s %10.2f (%d updates)\n",
            provider, updatePerf.getOpsPerSec("UPDATE"), updateIdsSeq.size());

      TotalStats findUpdatedPerf =
            runEntityFindUpdated(runningTimeout, updateIdsSeq);
      printResult(isWarmup, "[provider=%s] Updated finds/s %10.2f\n",
            provider, findUpdatedPerf.getOpsPerSec("FIND_UPDATED"));

      TotalStats findQueryPerf = runEntityFindQuery(runningTimeout, isWarmup);
      printResult(isWarmup, "[provider=%s] Query finds/s %10.2f\n",
            provider, findQueryPerf.getOpsPerSec("FIND_QUERY"));

      TotalStats findRandomPerf = runEntityFindRandom(runningTimeout);
      printResult(isWarmup, "[provider=%s] Random finds/s %10.2f\n",
            provider, findRandomPerf.getOpsPerSec("FIND_RANDOM"));

      // Get all entity ids
      List<Integer> entityIds = new ArrayList<Integer>();
      for (int i = 1; i <= numEntities; i++) entityIds.add(i);

      // Shuffle them
      Collections.shuffle(entityIds);

      // Add them to the queue delete consumption
      removeIds.addAll(entityIds);

      TotalStats deletePerf = runEntityDelete(runningTimeout);
      printResult(isWarmup, "[provider=%s] Deletes/s %10.2f\n",
            provider, deletePerf.getOpsPerSec("DELETE"));

      // TODO Print 2LC statistics...
   }

   static void printResult(boolean isWarmup, String format, Object... args) {
      if (!isWarmup) System.out.printf(format, args);
   }

   Long countEntities() {
      try {
         return withTx(tm, new Callable<Long>() {
            @Override
            public Long call() throws Exception {
               Session s = sessionFactory.openSession();
               Query query = s.createQuery("select count(*) from Family");
               Object result = query.list().get(0);
               s.close();
               return (Long) result;
            }
         });
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }


   TotalStats runEntityInsert(long runningTimeout) {
      return runSingleWork(runningTimeout, "insert", insertOperation());
   }

   TotalStats runEntityUpdate(long runningTimeout) {
      return runSingleWork(runningTimeout, "update", updateOperation());
   }

   TotalStats runEntityFindUpdated(long runningTimeout,
         List<Integer> updatedIdsSeq) {
      return runSingleWork(runningTimeout, "find-updated", findUpdatedOperation(updatedIdsSeq));
   }

   TotalStats runEntityFindQuery(long runningTimeout, boolean warmup) {
      return runSingleWork(runningTimeout, "find-query", findQueryOperation(warmup));
   }

   TotalStats runEntityFindRandom(long runningTimeout) {
      return runSingleWork(runningTimeout, "find-random", findRandomOperation());
   }

   TotalStats runEntityDelete(long runningTimeout) {
      return runSingleWork(runningTimeout, "remove", deleteOperation());
   }

   TotalStats runSingleWork(long runningTimeout, final String name, Operation op) {
      final TotalStats perf = new TotalStats();

      ExecutorService exec = Executors.newFixedThreadPool(
            NUM_THREADS, new ThreadFactory() {
         volatile int i = 0;
         @Override
         public Thread newThread(Runnable r) {
            return new Thread(new ThreadGroup(name),
                  r, "worker-" + name + "-" + i++);
         }
      });

      try {
         List<Future<Void>> futures = new ArrayList<Future<Void>>(NUM_THREADS);
         CyclicBarrier barrier = new CyclicBarrier(NUM_THREADS + 1);

         for (int i = 0; i < NUM_THREADS; i++)
            futures.add(exec.submit(
                  new WorkerThread(runningTimeout, perf, op, barrier)));

         try {
            barrier.await(); // wait for all threads to be ready
            barrier.await(); // wait for all threads to finish

            // Now check whether anything went wrong...
            for (Future<Void> future : futures) future.get();
         } catch (Exception e) {
            throw new RuntimeException(e);
         }

         return perf;
      } finally {
         exec.shutdown();
      }
   }

   <T> T captureThrowables(Callable<T> task) throws Exception {
      try {
         return task.call();
      } catch (Throwable t) {
         t.printStackTrace();
         if (t instanceof Exception)
            throw (Exception) t;
         else
            throw new RuntimeException(t);
      }
   }

   Operation insertOperation() {
      return new Operation("INSERT") {
         @Override
         boolean call(final int run) throws Exception {
            return captureThrowables(new Callable<Boolean>() {
               @Override
               public Boolean call() throws Exception {
                  return withTx(tm, new Callable<Boolean>() {
                     @Override
                     public Boolean call() throws Exception {
                        Session s = sessionFactory.openSession();
                        s.getTransaction().begin();

                        String name = "Zamarreño-" + run;
                        Family family = new Family(name);
                        s.persist(family);

                        s.getTransaction().commit();
                        s.close();
                        return true;
                     }
                  });
               }
            });
         }
      };
   }

   Operation updateOperation() {
      return new Operation("UPDATE") {
         @Override
         boolean call(final int run) throws Exception {
            return captureThrowables(new Callable<Boolean>() {
               @Override
               public Boolean call() throws Exception {
                  return withTx(tm, new Callable<Boolean>() {
                     @Override
                     public Boolean call() throws Exception {
                        Session s = sessionFactory.openSession();
                        s.getTransaction().begin();

                        // Update random entity that has been inserted
                        int id = RANDOM.nextInt(numEntities) + 1;
                        Family family = (Family) s.load(Family.class, id);
                        String newSecondName = "Arrizabalaga-" + run;
                        family.setSecondName(newSecondName);

                        s.getTransaction().commit();
                        s.close();
                        // Cache updated entities for later read
                        updatedIds.add(id);
                        return true;
                     }
                  });
               }
            });
         }
      };
   }

   Operation findUpdatedOperation(final List<Integer> updatedIdsSeq) {
      return new Operation("FIND_UPDATED") {
         @Override
         boolean call(final int run) throws Exception {
            return captureThrowables(new Callable<Boolean>() {
               @Override
               public Boolean call() throws Exception {
                  Session s = sessionFactory.openSession();

                  int id = updatedIdsSeq.get(RANDOM.nextInt(
                        updatedIdsSeq.size()));
                  Family family = (Family) s.load(Family.class, id);
                  String secondName = family.getSecondName();
                  assertNotNull(secondName);
                  assertTrue("Second name not expected: " + secondName,
                        secondName.startsWith("Arrizabalaga"));

                  s.close();
                  return true;
               }
            });
         }
      };
   }

   private Operation findQueryOperation(final boolean isWarmup) {
      return new Operation("FIND_QUERY") {
         @Override
         boolean call(final int run) throws Exception {
            return captureThrowables(new Callable<Boolean>() {
               @Override
               public Boolean call() throws Exception {
                  Session s = sessionFactory.openSession();

                  Query query = s.createQuery("from Family")
                        .setCacheable(true);
                  int maxResults = isWarmup ? 10 : 100;
                  query.setMaxResults(maxResults);
                  List<Family> result = (List<Family>) query.list();
                  assertEquals(maxResults, result.size());

                  s.close();
                  return true;
               }
            });
         }
      };
   }

   private Operation findRandomOperation() {
      return new Operation("FIND_RANDOM") {
         @Override
         boolean call(final int run) throws Exception {
            return captureThrowables(new Callable<Boolean>() {
               @Override
               public Boolean call() throws Exception {
                  Session s = sessionFactory.openSession();

                  int id = RANDOM.nextInt(numEntities) + 1;
                  Family family = (Family) s.load(Family.class, id);
                  String familyName = family.getName();
                  // Skip ñ check in order to avoid issues...
                  assertTrue("Unexpected family: " + familyName ,
                        familyName.startsWith("Zamarre"));

                  s.close();
                  return true;
               }
            });
         }
      };
   }

   private Operation deleteOperation() {
      return new Operation("DELETE") {
         @Override
         boolean call(final int run) throws Exception {
            return captureThrowables(new Callable<Boolean>() {
               @Override
               public Boolean call() throws Exception {
                  return withTx(tm, new Callable<Boolean>() {
                     @Override
                     public Boolean call() throws Exception {
                        Session s = sessionFactory.openSession();
                        s.getTransaction().begin();

                        // Get each id and remove it
                        int id = removeIds.poll();
                        Family family = (Family) s.load(Family.class, id);
                        String familyName = family.getName();
                        // Skip ñ check in order to avoid issues...
                        assertTrue("Unexpected family: " + familyName ,
                              familyName.startsWith("Zamarre"));
                        s.delete(family);

                        s.getTransaction().commit();
                        s.close();

                        return true;
                     }
                  });
               }
            });
         }
      };
   }

   public static Class[] getAnnotatedClasses() {
      return new Class[] {Family.class, Person.class, Address.class};
   }

   private static Metadata buildMetadata(StandardServiceRegistry registry) {
      final String cacheStrategy = "transactional";

      MetadataSources metadataSources = new MetadataSources( registry );
      for ( Class entityClass : getAnnotatedClasses() ) {
         metadataSources.addAnnotatedClass( entityClass );
      }

      Metadata metadata = metadataSources.buildMetadata();

      for ( PersistentClass entityBinding : metadata.getEntityBindings() ) {
         if (!entityBinding.isInherited()) {
            ( (RootClass) entityBinding ).setCacheConcurrencyStrategy( cacheStrategy);
         }
      }

      for ( Collection collectionBinding : metadata.getCollectionBindings() ) {
         collectionBinding.setCacheConcurrencyStrategy( cacheStrategy );
      }

      return metadata;
   }


   private static abstract class Operation {
      final String name;

      Operation(String name) {
         this.name = name;
      }

      abstract boolean call(int run) throws Exception;

   }

   private class WorkerThread implements Callable<Void> {
      private final long runningTimeout;
      private final TotalStats perf;
      private final Operation op;
      private final CyclicBarrier barrier;

      public WorkerThread(long runningTimeout, TotalStats perf,
            Operation op, CyclicBarrier barrier) {
         this.runningTimeout = runningTimeout;
         this.perf = perf;
         this.op = op;
         this.barrier = barrier;
      }

      @Override
      public Void call() throws Exception {
         // TODO: Extend barrier to capture start time
         barrier.await();
         try {
            long startNanos = System.nanoTime();
            long endNanos = startNanos + runningTimeout;
            int runs = 0;
            long missCount = 0;
            while (callOperation(endNanos, runs)) {
               boolean hit = op.call(runs);
               if (!hit) missCount++;
               runs++;
            }

            // TODO: Extend barrier to capture end time
            perf.addStats(op.name, runs,
                  System.nanoTime() - startNanos, missCount);
         } finally {
            barrier.await();
         }
         return null;
      }

      private boolean callOperation(long endNanos, int runs) {
         if (ALLOCATION) {
            return runs < RUN_COUNT_LIMIT;
         } else {
            return (runs & 0x400) != 0 || System.nanoTime() < endNanos;
         }
      }
   }

   private static class TotalStats {
      private ConcurrentHashMap<String, OpStats> statsMap =
            new ConcurrentHashMap<String, OpStats>();

      public void addStats(String opName, long opCount,
            long runningTime, long missCount) {
         OpStats s = new OpStats(opName, opCount, runningTime, missCount);
         OpStats old = statsMap.putIfAbsent(opName, s);
         boolean replaced = old == null;
         while (!replaced) {
            old = statsMap.get(opName);
            s = new OpStats(old, opCount, runningTime, missCount);
            replaced = statsMap.replace(opName, old, s);
         }
      }

      public double getOpsPerSec(String opName) {
         OpStats s = statsMap.get(opName);
         if (s == null) return 0;
         return s.opCount * 1000000000. / s.runningTime * s.threadCount;
      }

      public double getTotalOpsPerSec() {
         long totalOpCount = 0;
         long totalRunningTime = 0;
         long totalThreadCount = 0;
         for (Map.Entry<String, OpStats> e : statsMap.entrySet()) {
            OpStats s = e.getValue();
            totalOpCount += s.opCount;
            totalRunningTime += s.runningTime;
            totalThreadCount += s.threadCount;
         }
         return totalOpCount * 1000000000. / totalRunningTime * totalThreadCount;
      }

      public double getHitRatio(String opName) {
         OpStats s = statsMap.get(opName);
         if (s == null) return 0;
         return 1 - 1. * s.missCount / s.opCount;
      }

      public double getTotalHitRatio() {
         long totalOpCount = 0;
         long totalMissCount = 0;
         for (Map.Entry<String, OpStats> e : statsMap.entrySet()) {
            OpStats s = e.getValue();
            totalOpCount += s.opCount;
            totalMissCount += s.missCount;
         }
         return 1 - 1. * totalMissCount / totalOpCount;
      }
   }

   private static class OpStats {
      public final String opName;
      public final int threadCount;
      public final long opCount;
      public final long runningTime;
      public final long missCount;

      private OpStats(String opName, long opCount,
            long runningTime, long missCount) {
         this.opName = opName;
         this.threadCount = 1;
         this.opCount = opCount;
         this.runningTime = runningTime;
         this.missCount = missCount;
      }

      private OpStats(OpStats base, long opCount,
            long runningTime, long missCount) {
         this.opName = base.opName;
         this.threadCount = base.threadCount + 1;
         this.opCount = base.opCount + opCount;
         this.runningTime = base.runningTime + runningTime;
         this.missCount = base.missCount + missCount;
      }
   }

}

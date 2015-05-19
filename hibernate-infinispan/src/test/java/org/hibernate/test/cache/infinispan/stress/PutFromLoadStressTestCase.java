/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.stress;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.transaction.TransactionManager;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;

import org.hibernate.test.cache.infinispan.functional.Age;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import static org.infinispan.test.TestingUtil.withTx;
import static org.junit.Assert.assertFalse;

/**
 * A stress test for putFromLoad operations
 *
 * @author Galder Zamarreño
 * @since 4.1
 */
@Ignore
public class PutFromLoadStressTestCase {

   static final Log log = LogFactory.getLog(PutFromLoadStressTestCase.class);
   static final boolean isTrace = log.isTraceEnabled();
   static final int NUM_THREADS = 100;
   static final int WARMUP_TIME_SECS = 10;
   static final long RUNNING_TIME_SECS = Integer.getInteger("time", 60);
   static final long LAUNCH_INTERVAL_MILLIS = 10;

   static final int NUM_INSTANCES = 5000;

   static SessionFactory sessionFactory;
   static TransactionManager tm;

   final AtomicBoolean run = new AtomicBoolean(true);

   @BeforeClass
   public static void beforeClass() {
      // Extra options located in src/test/resources/hibernate.properties
      StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder()
              .applySetting( Environment.USE_SECOND_LEVEL_CACHE, "true" )
              .applySetting( Environment.USE_QUERY_CACHE, "true" )
              // TODO: Tweak to have a fully local region factory (no transport, cache mode = local, no marshalling, ...etc)
              .applySetting(
                      Environment.CACHE_REGION_FACTORY,
                      "org.hibernate.cache.infinispan.InfinispanRegionFactory"
              )
              .applySetting(
                      Environment.JTA_PLATFORM,
                      "org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform"
              )
              // Force minimal puts off to simplify stressing putFromLoad logic
              .applySetting( Environment.USE_MINIMAL_PUTS, "false" )
              .applySetting( Environment.HBM2DDL_AUTO, "create-drop" );

      StandardServiceRegistry serviceRegistry = ssrb.build();

      MetadataSources metadataSources = new MetadataSources( serviceRegistry )
              .addResource( "cache/infinispan/functional/Item.hbm.xml" )
              .addResource( "cache/infinispan/functional/Customer.hbm.xml" )
              .addResource( "cache/infinispan/functional/Contact.hbm.xml" )
              .addAnnotatedClass( Age.class );

      Metadata metadata = metadataSources.buildMetadata();
      for ( PersistentClass entityBinding : metadata.getEntityBindings() ) {
         if ( entityBinding instanceof RootClass ) {
            ( (RootClass) entityBinding ).setCacheConcurrencyStrategy( "transactional" );
         }
      }
      for ( Collection collectionBinding : metadata.getCollectionBindings() ) {
         collectionBinding.setCacheConcurrencyStrategy( "transactional" );
      }

      sessionFactory = metadata.buildSessionFactory();
      tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
   }

   @AfterClass
   public static void afterClass() {
      sessionFactory.close();
   }

   @Test
   public void testQueryPerformance() throws Exception {
      store();
//      doTest(true);
//      run.set(true); // Reset run
      doTest(false);
   }

   private void store() throws Exception {
      for (int i = 0; i < NUM_INSTANCES; i++) {
         final Age age = new Age();
         age.setAge(i);
         withTx(tm, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
               Session s = sessionFactory.openSession();
               s.getTransaction().begin();
               s.persist(age);
               s.getTransaction().commit();
               s.close();
               return null;
            }
         });
      }
   }

   private void doTest(boolean warmup) throws Exception {
      ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
      try {
         CyclicBarrier barrier = new CyclicBarrier(NUM_THREADS + 1);
         List<Future<String>> futures = new ArrayList<Future<String>>(NUM_THREADS);
         for (int i = 0; i < NUM_THREADS; i++) {
            Future<String> future = executor.submit(
                  new SelectQueryRunner(barrier, warmup, i + 1));
            futures.add(future);
            Thread.sleep(LAUNCH_INTERVAL_MILLIS);
         }
         barrier.await(); // wait for all threads to be ready

         long timeout = warmup ? WARMUP_TIME_SECS : RUNNING_TIME_SECS;
         TimeUnit unit = TimeUnit.SECONDS;

         Thread.sleep(unit.toMillis(timeout)); // Wait for the duration of the test
         run.set(false); // Instruct tests to stop doing work
         barrier.await(2, TimeUnit.MINUTES); // wait for all threads to finish

         log.infof("[%s] All threads finished, check for exceptions", title(warmup));
         for (Future<String> future : futures) {
            String opsPerMS = future.get();
            if (!warmup)
               log.infof("[%s] Operations/ms: %s", title(warmup), opsPerMS);
         }
         log.infof("[%s] All future gets checked", title(warmup));
      } catch (Exception e) {
         log.errorf(e, "Error in one of the execution threads during %s", title(warmup));
         throw e;
      } finally {
         executor.shutdownNow();
      }
   }

   private String title(boolean warmup) {
      return warmup ? "warmup" : "stress";
   }

   public class SelectQueryRunner implements Callable<String> {

      final CyclicBarrier barrier;
      final boolean warmup;
      final Integer customerId;

      public SelectQueryRunner(CyclicBarrier barrier, boolean warmup, Integer customerId) {
         this.barrier = barrier;
         this.warmup = warmup;
         this.customerId = customerId;
      }

      @Override
      public String call() throws Exception {
         try {
            if (isTrace)
               log.tracef("[%s] Wait for all executions paths to be ready to perform calls", title(warmup));
            barrier.await();

            long start = System.nanoTime();
            int runs = 0;
            if (isTrace) {
               log.tracef("[%s] Start time: %d", title(warmup), start);
            }

            queryItems();
            long end = System.nanoTime();
            long duration = end - start;
            if (isTrace)
               log.tracef("[%s] End time: %d, duration: %d, runs: %d",
                     title(warmup), start, duration, runs);

            return opsPerMS(duration, runs);
         } finally {
            if (isTrace)
               log.tracef("[%s] Wait for all execution paths to finish", title(warmup));

            barrier.await();
         }
      }

      private void deleteCachedItems() throws Exception {
         withTx(tm, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
               sessionFactory.getCache().evictEntityRegion(Age.class);
               return null;
            }
         });
      }

      private void queryItems() throws Exception {
         withTx(tm, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
               Session s = sessionFactory.getCurrentSession();
               Query query = s.getNamedQuery(Age.QUERY).setCacheable(true);
               List<Age> result = (List<Age>) query.list();
               assertFalse(result.isEmpty());
               return null;
            }
         });
      }

      private String opsPerMS(long nanos, int ops) {
         long totalMillis = TimeUnit.NANOSECONDS.toMillis(nanos);
         if (totalMillis > 0)
            return ops / totalMillis + " ops/ms";
         else
            return "NAN ops/ms";
      }

   }


}

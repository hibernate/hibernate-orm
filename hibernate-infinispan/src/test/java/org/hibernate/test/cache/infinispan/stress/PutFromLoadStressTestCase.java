package org.hibernate.test.cache.infinispan.stress;

import static org.infinispan.test.TestingUtil.withTx;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
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
import org.hibernate.TruthValue;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.test.cache.infinispan.functional.Age;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

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
		Properties envProps = Environment.getProperties();
		envProps.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		envProps.setProperty( Environment.USE_QUERY_CACHE, "true" );
		// TODO: Tweak to have a fully local region factory (no transport, cache mode = local, no marshalling, ...etc)
		envProps.setProperty( Environment.CACHE_REGION_FACTORY, "org.hibernate.cache.infinispan.InfinispanRegionFactory" );
		envProps.setProperty( Environment.JTA_PLATFORM, "org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform" );
	
		// Force minimal puts off to simplify stressing putFromLoad logic
		envProps.setProperty( Environment.USE_MINIMAL_PUTS, "false" );
	
		// Create database schema in each run
		envProps.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
	
		// // Database settings
		// envProps.setProperty(Environment.DRIVER, "org.postgresql.Driver");
		// envProps.setProperty(Environment.URL, "jdbc:postgresql://localhost/hibernate");
		// envProps.setProperty(Environment.DIALECT, "org.hibernate.dialect.PostgreSQL82Dialect");
		// envProps.setProperty(Environment.USER, "hbadmin");
		// envProps.setProperty(Environment.PASS, "hbadmin");
		
		StandardServiceRegistryImpl registry = ServiceRegistryBuilder.buildServiceRegistry(envProps);
	    MetadataSources sources = new MetadataSources( registry );
	
		// Mappings
		String[] mappings = { "cache/infinispan/functional/Item.hbm.xml", "cache/infinispan/functional/Customer.hbm.xml",
				"cache/infinispan/functional/Contact.hbm.xml" };
		for ( String mapping : mappings )
			sources.addResource( "org/hibernate/test/" + mapping );
	
		Class<?>[] annotatedClasses = getAnnotatedClasses();
		if ( annotatedClasses != null ) {
			for ( Class<?> annotatedClass : annotatedClasses ) {
				sources.addAnnotatedClass( annotatedClass );
			}
		}
	
		Metadata metadata = sources.buildMetadata();
	      
		Iterator<EntityBinding> entityIter = metadata.getEntityBindings().iterator();
		while ( entityIter.hasNext() ) {
			EntityBinding binding = entityIter.next();
			binding.getHierarchyDetails().getCaching().setAccessType( AccessType.TRANSACTIONAL );
			binding.getHierarchyDetails().getCaching().setRequested( TruthValue.TRUE );
			binding.getHierarchyDetails().getCaching().setRegion( binding.getEntityName() );
			binding.getHierarchyDetails().getCaching().setCacheLazyProperties( true );
		}
		Iterator<PluralAttributeBinding> collectionIter = metadata.getCollectionBindings().iterator();
		while ( collectionIter.hasNext() ) {
			PluralAttributeBinding binding = collectionIter.next();
			binding.getCaching().setAccessType( AccessType.TRANSACTIONAL );
			binding.getCaching().setRequested( TruthValue.TRUE );
			binding.getCaching()
					.setRegion( StringHelper.qualify( binding.getContainer().seekEntityBinding().getEntityName(), binding.getAttribute().getName() ) );
			binding.getCaching().setCacheLazyProperties( true );
		}
	
		sessionFactory =  metadata.buildSessionFactory();
	
		tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
	}

   @AfterClass
   public static void afterClass() {
      sessionFactory.close();
   }

   public static Class<Object>[] getAnnotatedClasses() {
      return new Class[] {Age.class};
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
            if (isTrace)
               log.tracef("[%s] Start time: %d", title(warmup), start);

//            while (USE_TIME && PutFromLoadStressTestCase.this.run.get()) {
//               if (runs % 100000 == 0)
//                  log.infof("[%s] Query run # %d", title(warmup), runs);
//
////               Customer customer = query();
////               deleteCached(customer);

               queryItems();
//               deleteCachedItems();
//
//               runs++;
//            }
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
//               Query query = s.createQuery("from Age").setCacheable(true);
               List<Age> result = (List<Age>) query.list();
               assertFalse(result.isEmpty());
               return null;
            }
         });
      }


//      private void deleteCachedItems() throws Exception {
//         withTx(tm, new Callable<Void>() {
//            @Override
//            public Void call() throws Exception {
//               sessionFactory.getCache().evictEntityRegion(Item.class);
//               return null;
//            }
//         });
//      }
//
//      private void queryItems() throws Exception {
//         withTx(tm, new Callable<Void>() {
//            @Override
//            public Void call() throws Exception {
//               Session s = sessionFactory.getCurrentSession();
//               Query query = s.createQuery("from Item").setCacheable(true);
//               List<Item> result = (List<Item>) query.list();
//               assertFalse(result.isEmpty());
//               return null;
//            }
//         });
//      }

//      private Customer query() throws Exception {
//         return withTx(tm, new Callable<Customer>() {
//            @Override
//            public Customer call() throws Exception {
//               Session s = sessionFactory.getCurrentSession();
//               Customer customer = (Customer) s.load(Customer.class, customerId);
//               assertNotNull(customer);
//               Set<Contact> contacts = customer.getContacts();
//               Contact contact = contacts.iterator().next();
//               assertNotNull(contact);
//               assertEquals("private contact", contact.getName());
//
////               Contact found = contacts.isEmpty() ? null : contacts.iterator().next();
////               Set<Contact> contacts = found.getContacts();
////               assertTrue(contacts + " not empty", contacts.isEmpty());
////
////               if (found != null && found.hashCode() == System.nanoTime()) {
////                  System.out.print(" ");
////               } else if (found == null) {
////                  throw new IllegalStateException("Contact cannot be null");
////               }
//               return customer;
//            }
//         });
//      }

//      private void deleteCached(final Customer customer) throws Exception {
//         withTx(tm, new Callable<Void>() {
//            @Override
//            public Void call() throws Exception {
//               sessionFactory.getCache().evictEntity(Customer.class, customer.getId());
//               return null;  // TODO: Customise this generated block
//            }
//         });
//      }

      private String opsPerMS(long nanos, int ops) {
         long totalMillis = TimeUnit.NANOSECONDS.toMillis(nanos);
         if (totalMillis > 0)
            return ops / totalMillis + " ops/ms";
         else
            return "NAN ops/ms";
      }

   }


}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.cache.infinispan.stress;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.transaction.RollbackException;
import javax.transaction.TransactionManager;

import org.hibernate.LockMode;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.PessimisticLockException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.access.InvalidationCacheAccessDelegate;
import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.RegionAccessStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.jta.JtaAwareConnectionProviderImpl;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.CustomParameterized;
import org.hibernate.test.cache.infinispan.stress.entities.Address;
import org.hibernate.test.cache.infinispan.stress.entities.Family;
import org.hibernate.test.cache.infinispan.stress.entities.Person;
import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;
import org.infinispan.configuration.cache.InterceptorConfiguration;
import org.infinispan.test.fwk.TestResourceTracker;
import org.infinispan.util.concurrent.TimeoutException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.infinispan.commands.VisitableCommand;
import org.infinispan.commands.tx.CommitCommand;
import org.infinispan.commands.tx.RollbackCommand;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.base.BaseCustomInterceptor;
import org.infinispan.remoting.RemoteException;

/**
 * Tries to execute random operations for {@link #EXECUTION_TIME} and then verify the log for correctness.
 *
 * Assumes serializable consistency.
 *
 * @author Radim Vansa
 */
@RunWith(CustomParameterized.class)
public abstract class CorrectnessTestCase {
   static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog(CorrectnessTestCase.class);
   static final long EXECUTION_TIME = TimeUnit.MINUTES.toMillis(2);
   static final int NUM_NODES = 4;
   static final int NUM_THREADS_PER_NODE = 4;
   static final int NUM_THREADS = NUM_NODES * NUM_THREADS_PER_NODE;
   static final int NUM_FAMILIES = 1;
   static final int NUM_ACCESS_AFTER_REMOVAL = NUM_THREADS * 2;
   static final int MAX_MEMBERS = 10;
   private final static Comparator<Log<?>> WALL_CLOCK_TIME_COMPARATOR = (o1, o2) -> Long.compare(o1.wallClockTime, o2.wallClockTime);

   private final static boolean INVALIDATE_REGION = Boolean.getBoolean("testInfinispan.correctness.invalidateRegion");
   private final static boolean INJECT_FAILURES = Boolean.getBoolean("testInfinispan.correctness.injectFailures");

   @Parameterized.Parameter(0)
   public String name;

   @Parameterized.Parameter(1)
   public CacheMode cacheMode;

   @Parameterized.Parameter(2)
   public AccessType accessType;

   static ThreadLocal<Integer> threadNode = new ThreadLocal<>();

   final AtomicInteger timestampGenerator = new AtomicInteger();
   final ConcurrentSkipListMap<Integer, AtomicInteger> familyIds = new ConcurrentSkipListMap<>();
   SessionFactory[] sessionFactories;
   volatile boolean running = true;

   final ThreadLocal<Map<Integer, List<Log<String>>>> familyNames = new ThreadLocal<Map<Integer, List<Log<String>>>>() {
      @Override
      protected Map<Integer, List<Log<String>>> initialValue() {
         return new HashMap<>();
      }
   };
   final ThreadLocal<Map<Integer, List<Log<Set<String>>>>> familyMembers = new ThreadLocal<Map<Integer, List<Log<Set<String>>>>>() {
      @Override
      protected Map<Integer, List<Log<Set<String>>>> initialValue() {
         return new HashMap<>();
      }
   };
   private BlockingDeque<Exception> exceptions = new LinkedBlockingDeque<>();

   public String getDbName() {
      return getClass().getName().replaceAll("\\W", "_");
   }

   @Ignore
   public static class Jta extends CorrectnessTestCase {
      private final TransactionManager transactionManager  = TestingJtaPlatformImpl.transactionManager();

      @Parameterized.Parameters(name = "{0}")
      public List<Object[]> getParameters() {
         return Arrays.<Object[]>asList(
               new Object[] { "transactional, invalidation", CacheMode.INVALIDATION_SYNC, AccessType.TRANSACTIONAL },
               new Object[] { "read-only, invalidation", CacheMode.INVALIDATION_SYNC, AccessType.READ_ONLY }, // maybe not needed
               new Object[] { "read-write, invalidation", CacheMode.INVALIDATION_SYNC, AccessType.READ_WRITE },
               new Object[] { "read-write, replicated", CacheMode.REPL_SYNC, AccessType.READ_WRITE },
               new Object[] { "read-write, distributed", CacheMode.DIST_SYNC, AccessType.READ_WRITE },
               new Object[] { "non-strict, replicated", CacheMode.REPL_SYNC, AccessType.NONSTRICT_READ_WRITE }
         );
      }

      @Override
      protected void applySettings(StandardServiceRegistryBuilder ssrb) {
         super.applySettings(ssrb);
         ssrb.applySetting( Environment.JTA_PLATFORM, TestingJtaPlatformImpl.class.getName() );
         ssrb.applySetting( Environment.CONNECTION_PROVIDER, JtaAwareConnectionProviderImpl.class.getName() );
         ssrb.applySetting( Environment.TRANSACTION_COORDINATOR_STRATEGY, JtaTransactionCoordinatorBuilderImpl.class.getName() );
      }

      @Override
      protected Operation getOperation() {
         if (accessType == AccessType.READ_ONLY) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            Operation operation;
            int r = random.nextInt(30);
            if (r == 0 && INVALIDATE_REGION) operation = new InvalidateCache();
            else if (r < 5) operation = new QueryFamilies();
            else if (r < 10) operation = new RemoveFamily(r < 12);
            else operation = new ReadFamily(r < 20);
            return operation;
         } else {
            return super.getOperation();
         }
      }
   }

   @Ignore
   public static class NonJta extends CorrectnessTestCase {
      @Parameterized.Parameters(name = "{0}")
      public List<Object[]> getParameters() {
         return Arrays.<Object[]>asList(
               new Object[] { "read-write, invalidation", CacheMode.INVALIDATION_SYNC, AccessType.READ_WRITE },
               new Object[] { "read-write, replicated", CacheMode.REPL_SYNC, AccessType.READ_WRITE },
               new Object[] { "read-write, distributed", CacheMode.DIST_SYNC, AccessType.READ_WRITE },
               new Object[] { "non-strict, replicated", CacheMode.REPL_SYNC, AccessType.NONSTRICT_READ_WRITE }
         );
      }

      @Override
      protected void applySettings(StandardServiceRegistryBuilder ssrb) {
         super.applySettings(ssrb);
         ssrb.applySetting(Environment.JTA_PLATFORM, NoJtaPlatform.class.getName());
         ssrb.applySetting(Environment.TRANSACTION_COORDINATOR_STRATEGY, JdbcResourceLocalTransactionCoordinatorBuilderImpl.class.getName());
      }
   }

   @BeforeClassOnce
   public void beforeClass() {
      TestResourceTracker.testStarted(getClass().getSimpleName());
      Arrays.asList(new File(System.getProperty("java.io.tmpdir"))
            .listFiles((dir, name) -> name.startsWith("family_") || name.startsWith("invalidations-")))
            .stream().forEach(f -> f.delete());
      StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder().enableAutoClose()
              .applySetting( Environment.USE_SECOND_LEVEL_CACHE, "true" )
              .applySetting( Environment.USE_QUERY_CACHE, "true" )
              .applySetting( Environment.DRIVER, "org.h2.Driver" )
              .applySetting( Environment.URL, "jdbc:h2:mem:" + getDbName() + ";TRACE_LEVEL_FILE=4")
              .applySetting( Environment.DIALECT, H2Dialect.class.getName() )
              .applySetting( Environment.HBM2DDL_AUTO, "create-drop" )
              .applySetting( Environment.CACHE_REGION_FACTORY, FailingInfinispanRegionFactory.class.getName())
              .applySetting( TestInfinispanRegionFactory.CACHE_MODE, cacheMode )
              .applySetting( Environment.USE_MINIMAL_PUTS, "false" )
              .applySetting( Environment.GENERATE_STATISTICS, "false" );
      applySettings(ssrb);

      sessionFactories = new SessionFactory[NUM_NODES];
      for (int i = 0; i < NUM_NODES; ++i) {
         StandardServiceRegistry registry = ssrb.build();
         Metadata metadata = buildMetadata( registry );
         sessionFactories[i] = metadata.buildSessionFactory();
      }
   }

   protected void applySettings(StandardServiceRegistryBuilder ssrb) {
      ssrb.applySetting( Environment.DEFAULT_CACHE_CONCURRENCY_STRATEGY, accessType.getExternalName());
      ssrb.applySetting(TestInfinispanRegionFactory.TRANSACTIONAL, accessType == AccessType.TRANSACTIONAL);
   }

   @AfterClassOnce
   public void afterClass() {
      for (SessionFactory sf : sessionFactories) {
         if (sf != null) sf.close();
      }
      TestResourceTracker.testFinished(getClass().getSimpleName());
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

   public static class InducedException extends Exception {
      public InducedException(String message) {
         super(message);
      }
   }

   public static class FailureInducingInterceptor extends BaseCustomInterceptor {
      @Override
      protected Object handleDefault(InvocationContext ctx, VisitableCommand command) throws Throwable {
         // Failure in CommitCommand/RollbackCommand keeps some locks closed, therefore blocking the test
         if (!(command instanceof CommitCommand || command instanceof RollbackCommand)) {
            /* Introduce 5 % probability of failure */
            if (ThreadLocalRandom.current().nextInt(100) < 5) {
               throw new InducedException("Simulating failure somewhere");
            }
         }
         return super.handleDefault(ctx, command);
      }
   }

   public static class FailingInfinispanRegionFactory extends TestInfinispanRegionFactory {
      public FailingInfinispanRegionFactory(Properties properties) {
         super(properties);
      }

      @Override
      protected void amendCacheConfiguration(String cacheName, ConfigurationBuilder configurationBuilder) {
         super.amendCacheConfiguration(cacheName, configurationBuilder);
         configurationBuilder.transaction().cacheStopTimeout(1, TimeUnit.SECONDS);
         if (INJECT_FAILURES) {
            // failure to write into timestamps would cause failure even though both DB and cache has been updated
            if (!cacheName.equals("timestamps") && !cacheName.endsWith(InfinispanRegionFactory.DEF_PENDING_PUTS_RESOURCE)) {
               configurationBuilder.customInterceptors().addInterceptor()
                  .interceptorClass(FailureInducingInterceptor.class)
                  .position(InterceptorConfiguration.Position.FIRST);
               log.trace("Injecting FailureInducingInterceptor into " + cacheName);
            } else {
               log.trace("Not injecting into " + cacheName);
            }
         }
      }
   }

   private final static Class[][] EXPECTED = {
      { TransactionException.class, RollbackException.class, StaleObjectStateException.class },
      { TransactionException.class, RollbackException.class, PessimisticLockException.class },
      { TransactionException.class, RollbackException.class, LockAcquisitionException.class },
      { RemoteException.class, TimeoutException.class },
      { StaleStateException.class, PessimisticLockException.class},
      { StaleStateException.class, ObjectNotFoundException.class},
      { StaleStateException.class, ConstraintViolationException.class},
      { StaleStateException.class, LockAcquisitionException.class},
      { PersistenceException.class, ConstraintViolationException.class },
      { PersistenceException.class, LockAcquisitionException.class },
      { javax.persistence.PessimisticLockException.class, PessimisticLockException.class },
      { OptimisticLockException.class, StaleStateException.class },
      { PessimisticLockException.class },
      { StaleObjectStateException.class },
      { ObjectNotFoundException.class },
      { LockAcquisitionException.class }
   };

   @Test
   public void test() throws Exception {
      ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);

      Map<Integer, List<Log<String>>> allFamilyNames = new HashMap<>();
      Map<Integer, List<Log<Set<String>>>> allFamilyMembers = new HashMap<>();

      running = true;
      List<Future<Void>> futures = new ArrayList<>();
      for (int node = 0; node < NUM_NODES; ++node) {
         final int NODE = node;
         for (int i = 0; i < NUM_THREADS_PER_NODE; ++i) {
            final int I = i;
            futures.add(exec.submit(() -> {
               Thread.currentThread().setName("Node" + (char)('A' + NODE) + "-thread-" + I);
               threadNode.set(NODE);
               while (running) {
                  Operation operation;
                  if (familyIds.size() < NUM_FAMILIES) {
                     operation = new InsertFamily(ThreadLocalRandom.current().nextInt(5) == 0);
                  } else {
                     operation = getOperation();
                  }
                  try {
                     operation.run();
                  } catch (Exception e) {
                     // ignore exceptions from optimistic failures and induced exceptions
                     if (hasCause(e, InducedException.class)) {
                        continue;
                     } else if (Stream.of(EXPECTED).anyMatch(exceptions -> matches(e, exceptions))) {
                        continue;
                     }
                     exceptions.add(e);
                     log.error("Failed " + operation.getClass().getName(), e);
                  }
               }
               synchronized (allFamilyNames) {
                  for (Map.Entry<Integer, List<Log<String>>> entry : familyNames.get().entrySet()) {
                     List<Log<String>> list = allFamilyNames.get(entry.getKey());
                     if (list == null) allFamilyNames.put(entry.getKey(), list = new ArrayList<>());
                     list.addAll(entry.getValue());
                  }
                  for (Map.Entry<Integer, List<Log<Set<String>>>> entry : familyMembers.get().entrySet()) {
                     List<Log<Set<String>>> list = allFamilyMembers.get(entry.getKey());
                     if (list == null) allFamilyMembers.put(entry.getKey(), list = new ArrayList<>());
                     list.addAll(entry.getValue());
                  }
               }
               return null;
            }));
         }
      }
      Exception failure = exceptions.poll(EXECUTION_TIME, TimeUnit.SECONDS);
      if (failure != null) exceptions.addFirst(failure);
      running = false;
      exec.shutdown();
      if (!exec.awaitTermination(1000, TimeUnit.SECONDS)) throw new IllegalStateException();
      for (Future<Void> f : futures) {
         f.get(); // check for exceptions
      }
      checkForEmptyPendingPuts();
      log.infof("Generated %d timestamps%n", timestampGenerator.get());
      AtomicInteger created = new AtomicInteger();
      AtomicInteger removed = new AtomicInteger();
      ForkJoinPool threadPool = ForkJoinPool.commonPool();
      ArrayList<ForkJoinTask<?>> tasks = new ArrayList<>();
      for (Map.Entry<Integer, List<Log<String>>> entry : allFamilyNames.entrySet()) {
         tasks.add(threadPool.submit(() -> {
            int familyId = entry.getKey();
            List<Log<String>> list = entry.getValue();
            created.incrementAndGet();
            NavigableMap<Integer, List<Log<String>>> logByTime = getWritesAtTime(list);
            checkCorrectness("family_name-" + familyId + "-", list, logByTime);
            if (list.stream().anyMatch(l -> l.type == LogType.WRITE && l.getValue() == null)) {
               removed.incrementAndGet();
            }
         }));
      }
      for (Map.Entry<Integer, List<Log<Set<String>>>> entry : allFamilyMembers.entrySet()) {
         tasks.add(threadPool.submit(() -> {
            int familyId = entry.getKey();
            List<Log<Set<String>>> list = entry.getValue();
            NavigableMap<Integer, List<Log<Set<String>>>> logByTime = getWritesAtTime(list);
            checkCorrectness("family_members-" + familyId + "-", list, logByTime);
         }));
      }
      for (ForkJoinTask<?> task : tasks) {
         // with heavy logging this may have trouble to complete
         task.get(30, TimeUnit.SECONDS);
      }
      if (!exceptions.isEmpty()) {
         for (Exception e : exceptions) {
            log.error("Test failure", e);
         }
         throw new IllegalStateException("There were " + exceptions.size() + " exceptions");
      }
      log.infof("Created %d families, removed %d%n", created.get(), removed.get());
   }

   private static class DelayedInvalidators {
      final ConcurrentMap map;
      final Object key;

      public DelayedInvalidators(ConcurrentMap map, Object key) {
         this.map = map;
         this.key = key;
      }

      public Object getPendingPutMap() {
         return map.get(key);
      }
   }

   protected void checkForEmptyPendingPuts() throws Exception {
      Field pp = PutFromLoadValidator.class.getDeclaredField("pendingPuts");
      pp.setAccessible(true);
      Method getInvalidators = null;
      List<DelayedInvalidators> delayed = new LinkedList<>();
      for (int i = 0; i < sessionFactories.length; i++) {
         SessionFactoryImplementor sfi = (SessionFactoryImplementor) sessionFactories[i];
         for (Object regionName : sfi.getCache().getSecondLevelCacheRegionNames()) {
            PutFromLoadValidator validator = getPutFromLoadValidator(sfi, (String) regionName);
            if (validator == null) {
               log.warn("No validator for " + regionName);
               continue;
            }
            ConcurrentMap<Object, Object> map = (ConcurrentMap) pp.get(validator);
            for (Iterator<Map.Entry<Object, Object>> iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
               Map.Entry entry = iterator.next();
               if (getInvalidators == null) {
                  getInvalidators = entry.getValue().getClass().getMethod("getInvalidators");
                  getInvalidators.setAccessible(true);
               }
               java.util.Collection invalidators = (java.util.Collection) getInvalidators.invoke(entry.getValue());
               if (invalidators != null && !invalidators.isEmpty()) {
                  delayed.add(new DelayedInvalidators(map, entry.getKey()));
               }
            }
         }
      }
      // poll until all invalidations come
      long deadline = System.currentTimeMillis() + 30000;
      while (System.currentTimeMillis() < deadline) {
         iterateInvalidators(delayed, getInvalidators, (k, i) -> {});
         if (delayed.isEmpty()) {
            break;
         }
         Thread.sleep(1000);
      }
      if (!delayed.isEmpty()) {
         iterateInvalidators(delayed, getInvalidators, (k, i) -> log.warnf("Left invalidators on key %s: %s", k, i));
         throw new IllegalStateException("Invalidators were not cleared: " + delayed.size());
      }
   }

   private void iterateInvalidators(List<DelayedInvalidators> delayed, Method getInvalidators, BiConsumer<Object, java.util.Collection> invalidatorConsumer) throws IllegalAccessException, InvocationTargetException {
      for (Iterator<DelayedInvalidators> iterator = delayed.iterator(); iterator.hasNext(); ) {
         DelayedInvalidators entry = iterator.next();
         Object pendingPutMap = entry.getPendingPutMap();
         if (pendingPutMap == null) {
            iterator.remove();
         }
         else {
            java.util.Collection invalidators = (java.util.Collection) getInvalidators.invoke(pendingPutMap);
            if (invalidators == null || invalidators.isEmpty()) {
               iterator.remove();
            }
            invalidatorConsumer.accept(entry.key, invalidators);
         }
      }
   }

   private boolean hasCause(Throwable throwable, Class<? extends Throwable> clazz) {
      if (throwable == null) return false;
      Throwable cause = throwable.getCause();
      if (throwable == cause) return false;
      if (clazz.isInstance(cause)) return true;
      return hasCause(cause, clazz);
   }

   private boolean matches(Throwable throwable, Class[] classes) {
      return matches(throwable, classes, 0);
   }

   private boolean matches(Throwable throwable, Class[] classes, int index) {
      return index >= classes.length || (classes[index].isInstance(throwable) && matches(throwable.getCause(), classes, index + 1));
   }

   protected Operation getOperation() {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      Operation operation;
      int r = random.nextInt(100);
      if (r == 0 && INVALIDATE_REGION) operation = new InvalidateCache();
      else if (r < 5) operation = new QueryFamilies();
      else if (r < 10) operation = new RemoveFamily(r < 6);
      else if (r < 20) operation = new UpdateFamily(r < 12, random.nextInt(1, 3));
      else if (r < 35) operation = new AddMember(r < 25);
      else if (r < 50) operation = new RemoveMember(r < 40);
      else operation = new ReadFamily(r < 75);
      return operation;
   }

   private <T> NavigableMap<Integer, List<Log<T>>> getWritesAtTime(List<Log<T>> list) {
      NavigableMap<Integer, List<Log<T>>> writes = new TreeMap<>();
      for (Log log : list) {
         if (log.type != LogType.WRITE) continue;
         for (int time = log.before; time <= log.after; ++time) {
            List<Log<T>> onTime = writes.get(time);
            if (onTime == null) {
               writes.put(time, onTime = new ArrayList<>());
            }
            onTime.add(log);
         }
      }
      return writes;
   }

   private <T> void checkCorrectness(String dumpPrefix, List<Log<T>> logs, NavigableMap<Integer, List<Log<T>>> writesByTime) {
      Collections.sort(logs, WALL_CLOCK_TIME_COMPARATOR);
      int nullReads = 0, reads = 0, writes = 0;
      for (Log read : logs) {
         if (read.type != LogType.READ) {
            writes++;
            continue;
         }
         if (read.getValue() == null || isEmptyCollection(read)) nullReads++;
         else reads++;

         Map<T, Log<T>> possibleValues = new HashMap<>();
         for (List<Log<T>> list : writesByTime.subMap(read.before, true, read.after, true).values()) {
            for (Log<T> write : list) {
               if (read.precedes(write)) continue;
               possibleValues.put(write.getValue(), write);
            }
         }
         int startOfLastWriteBeforeRead = 0;
         for (Map.Entry<Integer, List<Log<T>>> entry : writesByTime.headMap(read.before, false).descendingMap().entrySet()) {
            int time = entry.getKey();
            if (time < startOfLastWriteBeforeRead) break;
            for (Log<T> write : entry.getValue()) {
               if (write.after < read.before && write.before > startOfLastWriteBeforeRead) {
                  startOfLastWriteBeforeRead = write.before;
               }
               possibleValues.put(write.getValue(), write);
            }
         }

         if (possibleValues.isEmpty()) {
            // the entry was not created at all (first write failed)
            break;
         }
         if (!possibleValues.containsKey(read.getValue())) {
            dumpLogs(dumpPrefix, logs);
            exceptions.add(new IllegalStateException(String.format("R %s: %d .. %d (%s, %s) -> %s not in %s (%d+)", dumpPrefix,
                  read.before, read.after, read.threadName, new SimpleDateFormat("HH:mm:ss,SSS").format(new Date(read.wallClockTime)),
                  read.getValue(), possibleValues.values(), startOfLastWriteBeforeRead)));
            break;
         }
      }
      log.infof("Checked %d null reads, %d reads and %d writes%n", nullReads, reads, writes);
   }

   private <T> void dumpLogs(String prefix, List<Log<T>> logs) {
      try {
         File f = File.createTempFile(prefix,  ".log");
         log.info("Dumping logs into " + f.getAbsolutePath());
         try (BufferedWriter writer = Files.newBufferedWriter(f.toPath())) {
            for (Log<T> log : logs) {
               writer.write(log.toString());
               writer.write('\n');
            }
         }
      } catch (IOException e) {
         log.error("Failed to dump family logs");
      }
   }

   private static boolean isEmptyCollection(Log read) {
      return read.getValue() instanceof java.util.Collection && ((java.util.Collection) read.getValue()).isEmpty();
   }

   private abstract class Operation {
      protected final boolean rolledBack;

      public Operation(boolean rolledBack) {
         this.rolledBack = rolledBack;
      }

      public abstract void run() throws Exception;

      protected void withSession(Consumer<Session> consumer) throws Exception {
         int node = threadNode.get();
         Session s = sessionFactory(node).openSession();
         Transaction tx = s.getTransaction();
         tx.begin();
         try {
            consumer.accept(s);
         } catch (Exception e) {
            tx.markRollbackOnly();
            throw e;
         } finally {
            try {
               if (!rolledBack && tx.getStatus() == TransactionStatus.ACTIVE) {
                  log.trace("Hibernate commit begin");
                  tx.commit();
                  log.trace("Hibernate commit end");
               } else {
                  log.trace("Hibernate rollback begin");
                  tx.rollback();
                  log.trace("Hibernate rollback end");
               }
            } catch (Exception e) {
               log.trace("Hibernate commit or rollback failed, status is " + tx.getStatus(), e);
               if (tx.getStatus() == TransactionStatus.MARKED_ROLLBACK) {
                  tx.rollback();
               }
               throw e;
            } finally {
               // cannot close before XA commit since force increment requires open connection
               s.close();
            }
         }
      }

      protected void withRandomFamily(BiConsumer<Session, Family> consumer, Ref<String> familyNameUpdate, Ref<Set<String>> familyMembersUpdate, LockMode lockMode) throws Exception {
         int id = randomFamilyId(ThreadLocalRandom.current());
         int before = timestampGenerator.getAndIncrement();
         log.tracef("Started %s(%d, %s) at %d", getClass().getSimpleName(), id, rolledBack, before);
         Log<String> familyNameLog = new Log<>();
         Log<Set<String>> familyMembersLog = new Log<>();

         boolean failure = false;
         try {
            withSession(s -> {
               Family f = lockMode != null ? s.get(Family.class, id, lockMode) : s.get(Family.class, id);
               if (f == null) {
                  familyNameLog.setValue(null);
                  familyMembersLog.setValue(Collections.EMPTY_SET);
                  familyNotFound(id);
               } else {
                  familyNameLog.setValue(f.getName());
                  familyMembersLog.setValue(membersToNames(f.getMembers()));
                  consumer.accept(s, f);
               }
            });
         } catch (Exception e) {
            failure = true;
            throw e;
         } finally {
            int after = timestampGenerator.getAndIncrement();
            recordReadWrite(id, before, after, failure, familyNameUpdate, familyMembersUpdate, familyNameLog, familyMembersLog);
         }
      }

      protected void withRandomFamilies(int numFamilies, BiConsumer<Session, Family[]> consumer, String[] familyNameUpdates, Set<String>[] familyMembersUpdates, LockMode lockMode) throws Exception {
         int ids[] = new int[numFamilies];
         Log<String>[] familyNameLogs = new Log[numFamilies];
         Log<Set<String>>[] familyMembersLogs = new Log[numFamilies];
         for (int i = 0; i < numFamilies; ++i) {
            ids[i] = randomFamilyId(ThreadLocalRandom.current());
            familyNameLogs[i] = new Log<>();
            familyMembersLogs[i] = new Log<>();
         }
         int before = timestampGenerator.getAndIncrement();
         log.tracef("Started %s(%s) at %d", getClass().getSimpleName(), Arrays.toString(ids), before);

         boolean failure = false;
         try {
            withSession(s -> {
               Family[] families = new Family[numFamilies];
               for (int i = 0; i < numFamilies; ++i) {
                  Family f = lockMode != null ? s.get(Family.class, ids[i], lockMode) : s.get(Family.class, ids[i]);
                  families[i] = f;
                  if (f == null) {
                     familyNameLogs[i].setValue(null);
                     familyMembersLogs[i].setValue(Collections.EMPTY_SET);
                     familyNotFound(ids[i]);
                  } else {
                     familyNameLogs[i].setValue(f.getName());
                     familyMembersLogs[i].setValue(membersToNames(f.getMembers()));
                  }
               }
               consumer.accept(s, families);
            });
         } catch (Exception e) {
            failure = true;
            throw e;
         } finally {
            int after = timestampGenerator.getAndIncrement();
            for (int i = 0; i < numFamilies; ++i) {
               recordReadWrite(ids[i], before, after, failure,
                     familyNameUpdates != null ? Ref.of(familyNameUpdates[i]) : Ref.empty(),
                     familyMembersUpdates != null ? Ref.of(familyMembersUpdates[i]) : Ref.empty(),
                     familyNameLogs[i], familyMembersLogs[i]);
            }
         }
      }

      private void recordReadWrite(int id, int before, int after, boolean failure, Ref<String> familyNameUpdate, Ref<Set<String>> familyMembersUpdate, Log<String> familyNameLog, Log<Set<String>> familyMembersLog) {
         log.tracef("Finished %s at %d", getClass().getSimpleName(), after);

         LogType readType, writeType;
         if (failure || rolledBack) {
            writeType = LogType.WRITE_FAILURE;
            readType = LogType.READ_FAILURE;
         } else {
            writeType = LogType.WRITE;
            readType = LogType.READ;
         }

         familyNameLog.setType(readType).setTimes(before, after);
         familyMembersLog.setType(readType).setTimes(before, after);

         getRecordList(familyNames, id).add(familyNameLog);
         getRecordList(familyMembers, id).add(familyMembersLog);


         if (familyNameLog.getValue() != null) {
            if (familyNameUpdate.isSet()) {
               getRecordList(familyNames, id).add(new Log<>(before, after, familyNameUpdate.get(), writeType, familyNameLog));
            }
            if (familyMembersUpdate.isSet()) {
               getRecordList(familyMembers, id).add(new Log<>(before, after, familyMembersUpdate.get(), writeType, familyMembersLog));
            }
         }
      }
   }

   private class InsertFamily extends Operation {
      public InsertFamily(boolean rolledBack) {
         super(rolledBack);
      }

      @Override
      public void run() throws Exception {
         Family family = createFamily();
         int before = timestampGenerator.getAndIncrement();
         log.trace("Started InsertFamily at " + before);
         boolean failure = false;
         try {
            withSession(s -> s.persist(family));
         } catch (Exception e) {
            failure = true;
            throw e;
         } finally {
            int after = timestampGenerator.getAndIncrement();
            log.trace("Finished InsertFamily at " + after + ", " + (failure ? "failed" : "success"));
            familyIds.put(family.getId(), new AtomicInteger(NUM_ACCESS_AFTER_REMOVAL));
            LogType type = failure || rolledBack ? LogType.WRITE_FAILURE : LogType.WRITE;
            getRecordList(familyNames, family.getId()).add(new Log<>(before, after, family.getName(), type));
            getRecordList(familyMembers, family.getId()).add(new Log<>(before, after, membersToNames(family.getMembers()), type));
         }
      }
   }

   private Set<String> membersToNames(Set<Person> members) {
      return members.stream().map(p -> p.getFirstName()).collect(Collectors.toSet());
   }

   private class ReadFamily extends Operation {
      private final boolean evict;

      public ReadFamily(boolean evict) {
         super(false);
         this.evict = evict;
      }

      @Override
      public void run() throws Exception {
         withRandomFamily((s, f) -> {
               if (evict) {
                  sessionFactory(threadNode.get()).getCache().evictEntity(Family.class, f.getId());
               }
            }, Ref.empty(), Ref.empty(), null);
      }
   }

   private class UpdateFamily extends Operation {
      private final int numUpdates;

      public UpdateFamily(boolean rolledBack, int numUpdates) {
         super(rolledBack);
         this.numUpdates = numUpdates;
      }

      @Override
      public void run() throws Exception {
         String[] newNames = new String[numUpdates];
         for (int i = 0; i < numUpdates; ++i) {
            newNames[i] = randomString(ThreadLocalRandom.current());
         }
         withRandomFamilies(numUpdates, (s, families) -> {
            for (int i = 0; i < numUpdates; ++i) {
               Family f = families[i];
               if (f != null) {
                  f.setName(newNames[i]);
                  s.persist(f);
               }
            }
         }, newNames, null, LockMode.OPTIMISTIC_FORCE_INCREMENT);
      }
   }

   private class RemoveFamily extends Operation {
      public RemoveFamily(boolean rolledBack) {
         super(rolledBack);
      }

      @Override
      public void run() throws Exception {
         withRandomFamily((s, f) -> s.delete(f), Ref.of(null), Ref.of(Collections.EMPTY_SET), LockMode.OPTIMISTIC);
      }
   }

   private abstract class MemberOperation extends Operation {
      public MemberOperation(boolean rolledBack) {
         super(rolledBack);
      }

      @Override
      public void run() throws Exception {
         Ref<Set<String>> newMembers = new Ref<>();
         withRandomFamily((s, f) -> {
            boolean updated = updateMembers(s, ThreadLocalRandom.current(), f);
            if (updated) {
               newMembers.set(membersToNames(f.getMembers()));
               s.persist(f);
            }
         }, Ref.empty(), newMembers, LockMode.OPTIMISTIC_FORCE_INCREMENT);
      }

      protected abstract boolean updateMembers(Session s, ThreadLocalRandom random, Family f);
   }

   private class AddMember extends MemberOperation {
      public AddMember(boolean rolledBack) {
         super(rolledBack);
      }

      protected boolean updateMembers(Session s, ThreadLocalRandom random, Family f) {
         Set<Person> members = f.getMembers();
         if (members.size() < MAX_MEMBERS) {
            members.add(createPerson(random, f));
            return true;
         } else {
            return false;
         }
      }
   }

   private class RemoveMember extends MemberOperation {
      public RemoveMember(boolean rolledBack) {
         super(rolledBack);
      }

      @Override
      protected boolean updateMembers(Session s, ThreadLocalRandom random, Family f) {
         int numMembers = f.getMembers().size();
         if (numMembers > 0) {
            Iterator<Person> it = f.getMembers().iterator();
            Person person = null;
            for (int i = random.nextInt(numMembers); i >= 0; --i) {
               person = it.next();
            }
            it.remove();
            if (person != null) {
               s.delete(person);
            }
            return true;
         } else {
            return false;
         }
      }
   }

   private class QueryFamilies extends Operation {
      final static int MAX_RESULTS = 10;

      public QueryFamilies() {
         super(false);
      }

      @Override
      public void run() throws Exception {
         String prefix = new StringBuilder(2)
               .append((char) ThreadLocalRandom.current().nextInt('A', 'Z' + 1)).append('%').toString();
         int[] ids = new int[MAX_RESULTS];
         String[] names = new String[MAX_RESULTS];
         Set<String>[] members = new Set[MAX_RESULTS];

         int before = timestampGenerator.getAndIncrement();
         log.tracef("Started QueryFamilies at %d", before);
         withSession(s -> {
            List<Family> results = s.createCriteria(Family.class)
                  .add(Restrictions.like("name", prefix))
                  .setMaxResults(MAX_RESULTS)
                  .setCacheable(true)
                  .list();
            int index = 0;
            for (Family f : results) {
               ids[index] = f.getId();
               names[index] = f.getName();
               members[index] = membersToNames(f.getMembers());
               ++index;
            }
         });

         int after = timestampGenerator.getAndIncrement();
         log.tracef("Finished QueryFamilies at %d", after);
         for (int index = 0; index < MAX_RESULTS; ++index) {
            if (names[index] == null) break;
            getRecordList(familyNames, ids[index]).add(new Log<>(before, after, names[index], LogType.READ));
            getRecordList(familyMembers, ids[index]).add(new Log<>(before, after, members[index], LogType.READ));
         }
      }
   }

   private class InvalidateCache extends Operation {
      public InvalidateCache() {
         super(false);
      }

      @Override
      public void run() throws Exception {
         log.trace("Invalidating all caches");
         int node = threadNode.get();
         sessionFactory(node).getCache().evictAllRegions();
      }
   }

   private PutFromLoadValidator getPutFromLoadValidator(SessionFactoryImplementor sfi, String regionName) throws NoSuchFieldException, IllegalAccessException {
      RegionAccessStrategy strategy = sfi.getSecondLevelCacheRegionAccessStrategy(regionName);
      if (strategy == null) {
         return null;
      }
      Field delegateField = getField(strategy.getClass(), "delegate");
      Object delegate = delegateField.get(strategy);
      if (delegate == null) {
         return null;
      }
      if (InvalidationCacheAccessDelegate.class.isInstance(delegate)) {
         Field validatorField = InvalidationCacheAccessDelegate.class.getDeclaredField("putValidator");
         validatorField.setAccessible(true);
         return (PutFromLoadValidator) validatorField.get(delegate);
      } else {
         return null;
      }
   }

   private Field getField(Class<?> clazz, String fieldName) {
      Field f = null;
      while (clazz != null && clazz != Object.class) {
         try {
            f = clazz.getDeclaredField(fieldName);
            break;
         } catch (NoSuchFieldException e) {
            clazz = clazz.getSuperclass();
         }
      }
      if (f != null) {
         f.setAccessible(true);
      }
      return f;
   }

   protected SessionFactory sessionFactory(int node) {
      return sessionFactories[node];
   }

   private void familyNotFound(int id) {
      AtomicInteger access = familyIds.get(id);
      if (access == null) return;
      if (access.decrementAndGet() == 0) {
         familyIds.remove(id);
      }
   }

   private <T> List<T> getRecordList(ThreadLocal<Map<Integer, List<T>>> tlListMap, int id) {
      Map<Integer, List<T>> map = tlListMap.get();
      List<T> list = map.get(id);
      if (list == null) map.put(id, list = new ArrayList<>());
      return list;
   }

   private int randomFamilyId(ThreadLocalRandom random) {
      Map.Entry<Integer, AtomicInteger> first = familyIds.firstEntry();
      Map.Entry<Integer, AtomicInteger> last = familyIds.lastEntry();
      if (first == null || last == null) return 0;
      Map.Entry<Integer, AtomicInteger> ceiling = familyIds.ceilingEntry(random.nextInt(first.getKey(), last.getKey() + 1));
      return ceiling == null ? 0 : ceiling.getKey();
   }

   private static Family createFamily() {
      ThreadLocalRandom random = ThreadLocalRandom.current();
      String familyName = randomString(random);
      Family f = new Family(familyName);
      HashSet<Person> members = new HashSet<>();
      members.add(createPerson(random, f));
      f.setMembers(members);
      return f;
   }

   private static Person createPerson(ThreadLocalRandom random, Family family) {
      return new Person(randomString(random), family);
   }

   private static String randomString(ThreadLocalRandom random) {
      StringBuilder sb = new StringBuilder(10);
      for (int i = 0; i < 10; ++i) {
         sb.append((char) random.nextInt('A', 'Z' + 1));
      }
      return sb.toString();
   }

   private enum LogType {
      READ('R'), WRITE('W'), READ_FAILURE('L'), WRITE_FAILURE('F');

      private final char shortName;

      LogType(char shortName) {
         this.shortName = shortName;
      }
   }

   private class Log<T> {
      int before;
      int after;
      T value;
      LogType type;
      Log[] preceding;
      String threadName;
      long wallClockTime;

      public Log(int time) {
         this();
         this.before = time;
         this.after = time;
      }

      public Log(int before, int after, T value, LogType type, Log<T>... preceding) {
         this();
         this.before = before;
         this.after = after;
         this.value = value;
         this.type = type;
         this.preceding = preceding;
      }

      public Log() {
         threadName = Thread.currentThread().getName();
         wallClockTime = System.currentTimeMillis();
      }

      public Log setType(LogType type) {
         this.type = type;
         return this;
      }

      public void setTimes(int before, int after) {
         this.before = before;
         this.after = after;
      }

      public void setValue(T value) {
         this.value = value;
      }

      public T getValue() {
         return value;
      }

      public boolean precedes(Log<T> write) {
         if (write.preceding == null) return false;
         for (Log<T> l : write.preceding) {
            if (l == this) return true;
         }
         return false;
      }

      @Override
      public String toString() {
         return String.format("%c: %5d - %5d\t(%s,\t%s)\t%s", type.shortName, before, after,
               new SimpleDateFormat("HH:mm:ss,SSS").format(new Date(wallClockTime)), threadName, value);
      }
   }

   private static class Ref<T> {
      private static Ref EMPTY = new Ref() {
         @Override
         public void set(Object value) {
            throw new UnsupportedOperationException();
         }
      };
      private boolean set;
      private T value;

      public static <T> Ref<T> empty() {
         return EMPTY;
      }

      public static <T> Ref<T> of(T value) {
         Ref ref = new Ref();
         ref.set(value);
         return ref;
      }

      public boolean isSet() {
         return set;
      }

      public T get() {
         return value;
      }

      public void set(T value) {
         this.value = value;
         this.set = true;
      }
   }
}

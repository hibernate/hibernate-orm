/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.cache.infinispan.stress;

import javax.transaction.Status;
import javax.transaction.TransactionManager;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.test.cache.infinispan.stress.entities.Address;
import org.hibernate.test.cache.infinispan.stress.entities.Family;
import org.hibernate.test.cache.infinispan.stress.entities.Person;
import org.infinispan.util.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tries to execute random operations for {@link #EXECUTION_TIME} and then verify the log for correctness.
 *
 * Assumes serializable consistency.
 *
 * @author Radim Vansa
 */
@Ignore // as long-running test, we'll execute it only by hand
public class CorrectnessTestCase {
   static final org.infinispan.util.logging.Log log = LogFactory.getLog(CorrectnessTestCase.class);
   static final long EXECUTION_TIME = TimeUnit.MINUTES.toMillis(5);
   static final int NUM_THREADS = 10;
   static final int NUM_FAMILIES = 1;
   static final int NUM_ACCESS_AFTER_REMOVAL = NUM_THREADS * 2;
   static final String REMOVED = "__REMOVED__";

   AtomicInteger timestampGenerator = new AtomicInteger();
   ConcurrentSkipListMap<Integer, AtomicInteger> familyIds = new ConcurrentSkipListMap<>();
   SessionFactory sessionFactory;
   TransactionManager tm;
   volatile boolean running = true;

   ThreadLocal<Map<Integer, List<Log<String>>>> familyNames = new ThreadLocal<Map<Integer, List<Log<String>>>>() {
      @Override
      protected Map<Integer, List<Log<String>>> initialValue() {
         return new HashMap<>();
      }
   };
   ThreadLocal<Map<Integer, List<Log<Set<String>>>>> familyMembers = new ThreadLocal<Map<Integer, List<Log<Set<String>>>>>() {
      @Override
      protected Map<Integer, List<Log<Set<String>>>> initialValue() {
         return new HashMap<>();
      }
   };

   @Before
   public void beforeClass() {
      StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder().enableAutoClose()
              .applySetting( Environment.USE_SECOND_LEVEL_CACHE, "true" )
              .applySetting( Environment.USE_QUERY_CACHE, "true" )
              .applySetting( Environment.DRIVER, "org.h2.Driver" )
              .applySetting( Environment.URL, "jdbc:h2:mem:test")
              .applySetting( Environment.DIALECT, H2Dialect.class.getName() )
              .applySetting( Environment.HBM2DDL_AUTO, "create-drop" )
              .applySetting( Environment.CACHE_REGION_FACTORY, "org.hibernate.cache.infinispan.InfinispanRegionFactory" )
              .applySetting( Environment.JTA_PLATFORM, "org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform" )
              .applySetting( Environment.GENERATE_STATISTICS, "false" )
              ;

      StandardServiceRegistry registry = ssrb.build();

      Metadata metadata = buildMetadata( registry );

      sessionFactory = metadata.buildSessionFactory();

      tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
   }

   @After
   public void afterClass() {
      sessionFactory.close();
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

   @Test
   public void test() throws InterruptedException, ExecutionException {
      ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);

      Map<Integer, List<Log<String>>> allFamilyNames = new HashMap<>();
      Map<Integer, List<Log<Set<String>>>> allFamilyMembers = new HashMap<>();

      running = true;
      List<Future<Void>> futures = new ArrayList<>();
      for (int i = 0; i < NUM_THREADS; ++i) {
         futures.add(exec.submit(() -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            while (running) {
               Operation operation;
               if (familyIds.size() < NUM_FAMILIES) {
                  operation = new InsertFamily();
               } else {
                  int r = random.nextInt(100);
                  if (r == 0) operation = new InvalidateCache();
                  else if (r < 5) operation = new QueryFamilies();
                  else if (r < 10) operation = new RemoveFamily();
                  else if (r < 20) operation = new UpdateFamily();
                  else if (r < 35) operation = new AddMember();
                  else if (r < 50) operation = new RemoveMember();
                  else operation = new ReadFamily(r < 75);
               }
               try {
                  operation.run();
               } catch (Exception e) {
                  // ignore exceptions from optimistic failures
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
      Thread.sleep(EXECUTION_TIME);
      running = false;
      exec.shutdown();
      if (!exec.awaitTermination(1000, TimeUnit.SECONDS)) throw new IllegalStateException();
      for (Future<Void> f : futures) {
         f.get(); // check for exceptions
      }

      log.infof("Generated %d timestamps%n", timestampGenerator.get());
      AtomicInteger created = new AtomicInteger();
      AtomicInteger removed = new AtomicInteger();
      ForkJoinPool threadPool = ForkJoinPool.commonPool();
      ArrayList<ForkJoinTask<?>> tasks = new ArrayList<>();
      for (List<Log<String>> list : allFamilyNames.values()) {
         tasks.add(threadPool.submit(() -> {
            created.incrementAndGet();
            NavigableMap<Integer, List<Log<String>>> logByTime = getWritesAtTime(list);
            checkCorrectness(list, logByTime);
            if (list.stream().anyMatch(l -> !l.read && l.getValue() == null)) {
               removed.incrementAndGet();
            }
         }));
      }
      for (List<Log<Set<String>>> list : allFamilyMembers.values()) {
         tasks.add(threadPool.submit(() -> {
            NavigableMap<Integer, List<Log<Set<String>>>> logByTime = getWritesAtTime(list);
            checkCorrectness(list, logByTime);
         }));
      }
      for (ForkJoinTask<?> task : tasks) {
         task.get(); // propagate exception
      }
      log.infof("Created %d families, removed %d%n", created.get(), removed.get());
   }

   private <T> NavigableMap<Integer, List<Log<T>>> getWritesAtTime(List<Log<T>> list) {
      NavigableMap<Integer, List<Log<T>>> writes = new TreeMap<>();
      for (Log log : list) {
         if (log.read) continue;
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

   private <T> void checkCorrectness(List<Log<T>> logs, NavigableMap<Integer, List<Log<T>>> writesByTime) {
      int nullReads = 0, reads = 0, writes = 0;
      for (Log read : logs) {
         if (!read.read) {
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

         if (!possibleValues.containsKey(read.getValue())) {
            throw new IllegalStateException(String.format("R: %d .. %d (%s, %s) -> %s not in %s (%d+)",
                  read.before, read.after, read.threadName, new SimpleDateFormat("HH:mm:ss,SSS").format(new Date(read.wallClockTime)),
                  read.getValue(), possibleValues.values(), startOfLastWriteBeforeRead));
         }
      }
      log.infof("Checked %d null reads, %d reads and %d writes%n", nullReads, reads, writes);
   }

   private static boolean isEmptyCollection(Log read) {
      return read.getValue() instanceof java.util.Collection && ((java.util.Collection) read.getValue()).isEmpty();
   }

   private abstract class Operation {
      public abstract void run() throws Exception;

      protected void withSession(Consumer<Session> consumer) throws Exception {
         tm.begin();
         try {
            Session s = sessionFactory.openSession();
            s.getTransaction().begin();
            consumer.accept(s);
            s.getTransaction().commit();
            s.close();
         } catch (RuntimeException e) {
            tm.setRollbackOnly();
            throw e;
         } finally {
            if (tm.getStatus() == Status.STATUS_ACTIVE) {
               tm.commit();
            } else {
               tm.rollback();
            }
         }
      }

      protected void withRandomFamily(BiConsumer<Session, Family> consumer, Optional<String> familyNameUpdate, Optional<Set<String>> familyMembersUpdate) throws Exception {
         int id = randomFamilyId(ThreadLocalRandom.current());
         int before = timestampGenerator.getAndIncrement();
         log.tracef("Started %s at %d", getClass().getSimpleName(), before);
         Log<String> familyNameLog = new Log<>(true);
         Log<Set<String>> familyMembersLog = new Log<>(true);

         withSession(s -> {
            Family f = s.get(Family.class, id);
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

         int after = timestampGenerator.getAndIncrement();
         log.tracef("Finished %s at %d", getClass().getSimpleName(), after);
         familyNameLog.setTimes(before, after);
         familyMembersLog.setTimes(before, after);

         getRecordList(familyNames, id).add(familyNameLog);
         getRecordList(familyMembers, id).add(familyMembersLog);

         if (familyNameLog.getValue() != null) {
            if (familyNameUpdate.isPresent()) {
               getRecordList(familyNames, id).add(new Log<>(before, after, familyNameUpdate.get() == REMOVED ? null : familyNameUpdate.get(), false, familyNameLog));
            }
            if (familyMembersUpdate.isPresent()) {
               getRecordList(familyMembers, id).add(new Log<>(before, after, familyMembersUpdate.get(), false, familyMembersLog));
            }
         }
      }
   }

   private class InsertFamily extends Operation {
      @Override
      public void run() throws Exception {
         Family family = createFamily();
         int before = timestampGenerator.getAndIncrement();
         log.trace("Started InsertFamily at " + before);
         withSession(s -> s.persist(family));
         familyIds.put(family.getId(), new AtomicInteger(NUM_ACCESS_AFTER_REMOVAL));
         int after = timestampGenerator.getAndIncrement();
         log.trace("Finished InsertFamily at " + after);
         getRecordList(familyNames, family.getId()).add(new Log<>(before, after, family.getName(), false));
         getRecordList(familyMembers, family.getId()).add(new Log<>(before, after, membersToNames(family.getMembers()), false));
      }
   }

   private Set<String> membersToNames(Set<Person> members) {
      return members.stream().map(p -> p.getFirstName()).collect(Collectors.toSet());
   }

   private class ReadFamily extends Operation {
      private final boolean evict;

      public ReadFamily(boolean evict) {
         this.evict = evict;
      }

      @Override
      public void run() throws Exception {
         withRandomFamily((s, f) -> {
               if (evict) {
                  sessionFactory.getCache().evictEntity(Family.class, f.getId());
               }
            }, Optional.empty(), Optional.empty());
      }
   }

   private class UpdateFamily extends Operation {
      @Override
      public void run() throws Exception {
         String newName = randomString(ThreadLocalRandom.current());
         withRandomFamily((s, f) -> {
            f.setName(newName);
            s.persist(f);
         }, Optional.of(newName), Optional.empty());
      }
   }

   private class RemoveFamily extends Operation {
      @Override
      public void run() throws Exception {
         withRandomFamily((s, f) -> s.delete(f), Optional.of(REMOVED), Optional.of(Collections.EMPTY_SET));
      }
   }

   private abstract class MemberOperation extends Operation {
      @Override
      public void run() throws Exception {
         Set<String> newMembers = new HashSet<>();
         withRandomFamily((s, f) -> {
            updateMembers(s, ThreadLocalRandom.current(), f);
            newMembers.addAll(membersToNames(f.getMembers()));
            s.persist(f);
         }, Optional.empty(), Optional.of(newMembers));
      }

      protected abstract void updateMembers(Session s, ThreadLocalRandom random, Family f);
   }

   private class AddMember extends MemberOperation {
      protected void updateMembers(Session s, ThreadLocalRandom random, Family f) {
         f.getMembers().add(createPerson(random, f));
      }
   }

   private class RemoveMember extends MemberOperation {
      @Override
      protected void updateMembers(Session s, ThreadLocalRandom random, Family f) {
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
         }
      }
   }

   private class QueryFamilies extends Operation {
      final static int MAX_RESULTS = 10;

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
         log.tracef("Finsihed QueryFamilies at %d", after);
         for (int index = 0; index < MAX_RESULTS; ++index) {
            if (names[index] == null) break;
            getRecordList(familyNames, ids[index]).add(new Log<>(before, after, names[index], true));
            getRecordList(familyMembers, ids[index]).add(new Log<>(before, after, members[index], true));
         }
      }
   }

   private class InvalidateCache extends Operation {
      @Override
      public void run() throws Exception {
         log.trace("Invalidating all caches");
         tm.begin();
         try {
            sessionFactory.getCache().evictAllRegions();
         } catch (RuntimeException e) {
            tm.setRollbackOnly();
            throw e;
         } finally {
            if (tm.getStatus() == Status.STATUS_ACTIVE) {
               tm.commit();
            } else {
               tm.rollback();
            }
         }
      }
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
      Integer first = familyIds.firstKey();
      Integer last = familyIds.lastKey();
      if (first == null || last == null) return 0;
      return familyIds.ceilingKey(random.nextInt(first, last + 1));
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

   private class Log<T> {
      int before;
      int after;
      T value;
      boolean read;
      Log[] preceding;
      String threadName;
      long wallClockTime;

      public Log(int time) {
         this();
         this.before = time;
         this.after = time;
      }

      public Log(int before, int after, T value, boolean read, Log<T>... preceding) {
         this();
         this.before = before;
         this.after = after;
         this.value = value;
         this.read = read;
         this.preceding = preceding;
      }

      public Log(boolean read) {
         this();
         this.read = read;
      }

      protected Log() {
         threadName = Thread.currentThread().getName();
         wallClockTime = System.currentTimeMillis();
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
         return String.format("%c: %s (%s, %s), %d - %d", read ? 'R' : 'W', value, threadName,
               new SimpleDateFormat("HH:mm:ss,SSS").format(new Date(wallClockTime)), before, after);
      }
   }
}

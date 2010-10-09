/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat, Inc or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cache.infinispan.access;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.cache.CacheException;

/**
 * Encapsulates logic to allow a {@link TransactionalAccessDelegate} to determine
 * whether a {@link TransactionalAccessDelegate#putFromLoad(Object, Object, long, Object, boolean)
 * call should be allowed to update the cache. A <code>putFromLoad</code> has
 * the potential to store stale data, since the data may have been removed from the
 * database and the cache between the time when the data was read from the database 
 * and the actual call to <code>putFromLoad</code>.
 * <p>
 * The expected usage of this class by a thread that read the cache and did
 * not find data is:
 *
 * <ol>
 * <li> Call {@link #registerPendingPut(Object)}</li>
 * <li> Read the database</li>
 * <li> Call {@link #acquirePutFromLoadLock(Object)}
 * <li> if above returns <code>false</code>, the thread should not cache the data;
 *      only if above returns <code>true</code>, put data in the cache and...</li>
 * <li> then call {@link #releasePutFromLoadLock(Object)}</li>
 * </ol>
 * </p>
 *
 * <p>
 * The expected usage by a thread that is taking an action such that any pending
 * <code>putFromLoad</code> may have stale data and should not cache it is to either
 * call
 *
 * <ul>
 * <li> {@link #invalidateKey(Object)} (for a single key invalidation)</li>
 * <li>or {@link #invalidateRegion()} (for a general invalidation all pending puts)</li>
 * </ul>
 * </p>
 *
 * <p>
 * This class also supports the concept of "naked puts", which are calls to
 * {@link #acquirePutFromLoadLock(Object)} without a preceding {@link #registerPendingPut(Object)}
 * call.
 * </p>
 *
 * @author Brian Stansberry
 * 
 * @version $Revision: $
 */
public class PutFromLoadValidator {
   /**
    * Period (in ms) after a removal during which a call to
    * {@link #acquirePutFromLoadLock(Object)} that hasn't been
    * {@link #registerPendingPut(Object) pre-registered} (aka a "naked put")
    * will return false.
    * will return false.
    */
   public static final long NAKED_PUT_INVALIDATION_PERIOD = TimeUnit.SECONDS.toMillis(20); 

   /** Period (in ms) after which a pending put is placed in the over-age queue */
   private static final long PENDING_PUT_OVERAGE_PERIOD = TimeUnit.SECONDS.toMillis(5);

   /** Period (in ms) before which we stop trying to clean out pending puts */
   private static final long PENDING_PUT_RECENT_PERIOD = TimeUnit.SECONDS.toMillis(2);

   /** Period (in ms) after which a pending put is never expected to come in and should be cleaned */
   private static final long MAX_PENDING_PUT_DELAY = TimeUnit.SECONDS.toMillis(2 * 60);

   /**
    * Used to determine whether the owner of a pending put is a thread or a transaction
    */
   private final TransactionManager transactionManager;

   private final long nakedPutInvalidationPeriod;
   private final long pendingPutOveragePeriod;
   private final long pendingPutRecentPeriod;
   private final long maxPendingPutDelay;

   /**
    * Registry of expected, future, isPutValid calls. If a key+owner is registered in this map, it
    * is not a "naked put" and is allowed to proceed.
    */
   private final ConcurrentMap<Object, PendingPutMap> pendingPuts = new ConcurrentHashMap<Object, PendingPutMap>();
   /**
    * List of pending puts. Used to ensure we don't leak memory via the pendingPuts map
    */
   private final List<WeakReference<PendingPut>> pendingQueue = new LinkedList<WeakReference<PendingPut>>();
   /**
    * Separate list of pending puts that haven't been resolved within PENDING_PUT_OVERAGE_PERIOD.
    * Used to ensure we don't leak memory via the pendingPuts map. Tracked separately from more
    * recent pending puts for efficiency reasons.
    */
   private final List<WeakReference<PendingPut>> overagePendingQueue = new LinkedList<WeakReference<PendingPut>>();
   /** Lock controlling access to pending put queues */
   private final Lock pendingLock = new ReentrantLock();
   private final ConcurrentMap<Object, Long> recentRemovals = new ConcurrentHashMap<Object, Long>();
   /**
    * List of recent removals. Used to ensure we don't leak memory via the recentRemovals map
    */
   private final List<RecentRemoval> removalsQueue = new LinkedList<RecentRemoval>();
   /**
    * The time when the first element in removalsQueue will expire. No reason to do housekeeping on
    * the queue before this time.
    */
   private volatile long earliestRemovalTimestamp;
   /** Lock controlling access to removalsQueue */
   private final Lock removalsLock = new ReentrantLock();

   /**
    * The time of the last call to regionRemoved(), plus NAKED_PUT_INVALIDATION_PERIOD. All naked
    * puts will be rejected until the current time is greater than this value.
    */
   private volatile long invalidationTimestamp;

   /**
    * Creates a new PutFromLoadValidator.
    * 
    * @param transactionManager
    *           transaction manager to use to associate changes with a transaction; may be
    *           <code>null</code>
    */
   public PutFromLoadValidator(TransactionManager transactionManager) {
      this(transactionManager, NAKED_PUT_INVALIDATION_PERIOD, PENDING_PUT_OVERAGE_PERIOD,
               PENDING_PUT_RECENT_PERIOD, MAX_PENDING_PUT_DELAY);
   }

   /**
    * Constructor variant for use by unit tests; allows control of various timeouts by the test.
    */
   protected PutFromLoadValidator(TransactionManager transactionManager,
            long nakedPutInvalidationPeriod, long pendingPutOveragePeriod,
            long pendingPutRecentPeriod, long maxPendingPutDelay) {
      this.transactionManager = transactionManager;
      this.nakedPutInvalidationPeriod = nakedPutInvalidationPeriod;
      this.pendingPutOveragePeriod = pendingPutOveragePeriod;
      this.pendingPutRecentPeriod = pendingPutRecentPeriod;
      this.maxPendingPutDelay = maxPendingPutDelay;
   }

   // ----------------------------------------------------------------- Public

   /**
    * Acquire a lock giving the calling thread the right to put data in the
    * cache for the given key.
    * <p>
    * <strong>NOTE:</strong> A call to this method that returns <code>true</code>
    * should always be matched with a call to {@link #releasePutFromLoadLock(Object)}.
    * </p>
    *
    * @param key the key
    *
    * @return <code>true</code> if the lock is acquired and the cache put
    *         can proceed; <code>false</code> if the data should not be cached
    */
	public boolean acquirePutFromLoadLock(Object key) {
      boolean valid = false;
      boolean locked = false;
      long now = System.currentTimeMillis();

      // Important: Do cleanup before we acquire any locks so we
      // don't deadlock with invalidateRegion
      cleanOutdatedPendingPuts(now, true);

      try {
         PendingPutMap pending = pendingPuts.get(key);
         if (pending != null) {
            locked = pending.acquireLock(100, TimeUnit.MILLISECONDS);
            if (locked) {
               try {
                  PendingPut toCancel = pending.remove(getOwnerForPut());
                  if (toCancel != null) {
                     valid = !toCancel.completed;
                     toCancel.completed = true;
                  }
               }
               finally {
                  if (!valid) {
                     pending.releaseLock();
                     locked = false;
                  }
               }
            }
         }
         else {
            // Key wasn't in pendingPuts, so either this is a "naked put"
            // or regionRemoved has been called. Check if we can proceed
            if (now > invalidationTimestamp) {
               Long removedTime = recentRemovals.get(key);
               if (removedTime == null || now > removedTime.longValue()) {
                  // It's legal to proceed. But we have to record this key
                  // in pendingPuts so releasePutFromLoadLock can find it.
                  // To do this we basically simulate a normal "register
                  // then acquire lock" pattern
                  registerPendingPut(key);
                  locked = acquirePutFromLoadLock(key);
                  valid = locked;
               }
            }
         }
      }
      catch (Throwable t) {

         valid = false;

         if (locked) {
            PendingPutMap toRelease = pendingPuts.get(key);
            if (toRelease != null) {
               toRelease.releaseLock();
            }
         }

         if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
         } else if (t instanceof Error) {
            throw (Error) t;
         } else {
            throw new RuntimeException(t);
         }
      }

      return valid;
   }

   /**
    * Releases the lock previously obtained by a call to
    * {@link #acquirePutFromLoadLock(Object)} that returned <code>true</code>.
    *
    * @param key the key
    */
   public void releasePutFromLoadLock(Object key) {
      PendingPutMap pending = pendingPuts.get(key);
      if (pending != null) {
         if (pending.size() == 0) {
            pendingPuts.remove(key);
         }
         pending.releaseLock();
      }
   }

   /**
    * Invalidates any {@link #registerPendingPut(Object) previously registered pending puts} ensuring a subsequent call to
    * {@link #acquirePutFromLoadLock(Object)} will return <code>false</code>. <p> This method will block until any
    * concurrent thread that has {@link #acquirePutFromLoadLock(Object) acquired the putFromLoad lock} for the given key
    * has released the lock. This allows the caller to be certain the putFromLoad will not execute after this method
    * returns, possibly caching stale data. </p>
    *
    * @param key key identifying data whose pending puts should be invalidated
    * @return <code>true</code> if the invalidation was successful; <code>false</code> if a problem occured (which the
    *         caller should treat as an exception condition)
    */
   public boolean invalidateKey(Object key) {

      boolean success = true;

      // Invalidate any pending puts
      PendingPutMap pending = pendingPuts.get(key);
      if (pending != null) {
         // This lock should be available very quickly, but we'll be
         // very patient waiting for it as callers should treat not
         // acquiring it as an exception condition
         if (pending.acquireLock(60, TimeUnit.SECONDS)) {
            try {
               pending.invalidate();
            }
            finally {
               pending.releaseLock();
            }
         } else {
            success = false;
         }
      }

      // Record when this occurred to invalidate later naked puts
      RecentRemoval removal = new RecentRemoval(key, this.nakedPutInvalidationPeriod);
      recentRemovals.put(key, removal.timestamp);

      // Don't let recentRemovals map become a memory leak
      RecentRemoval toClean = null;
      boolean attemptClean = removal.timestamp.longValue() > earliestRemovalTimestamp;
      removalsLock.lock();
      try {
         removalsQueue.add(removal);

         if (attemptClean) {
            if (removalsQueue.size() > 1) { // we have at least one as we
               // just added it
               toClean = removalsQueue.remove(0);
            }
            earliestRemovalTimestamp = removalsQueue.get(0).timestamp.longValue();
         }
      } finally {
         removalsLock.unlock();
      }

      if (toClean != null) {
         Long cleaned = recentRemovals.get(toClean.key);
         if (cleaned != null && cleaned.equals(toClean.timestamp)) {
            cleaned = recentRemovals.remove(toClean.key);
            if (cleaned != null && cleaned.equals(toClean.timestamp) == false) {
               // Oops; removed the wrong timestamp; restore it
               recentRemovals.putIfAbsent(toClean.key, cleaned);
            }
         }
      }

      return success;
   }

   /**
    * Invalidates all {@link #registerPendingPut(Object) previously registered pending puts} ensuring a subsequent call to
    * {@link #acquirePutFromLoadLock(Object)} will return <code>false</code>. <p> This method will block until any
    * concurrent thread that has {@link #acquirePutFromLoadLock(Object) acquired the putFromLoad lock} for the any key has
    * released the lock. This allows the caller to be certain the putFromLoad will not execute after this method returns,
    * possibly caching stale data. </p>
    *
    * @return <code>true</code> if the invalidation was successful; <code>false</code> if a problem occured (which the
    *         caller should treat as an exception condition)
    */
   public boolean invalidateRegion() {

      boolean ok = false;
      invalidationTimestamp = System.currentTimeMillis() + this.nakedPutInvalidationPeriod;

      try {

         // Acquire the lock for each entry to ensure any ongoing
         // work associated with it is completed before we return
         for (PendingPutMap entry : pendingPuts.values()) {
            if (entry.acquireLock(60, TimeUnit.SECONDS)) {
               try {
                  entry.invalidate();
               }
               finally {
                  entry.releaseLock();
               }
            } else {
               ok = false;
            }
         }

         removalsLock.lock();
         try {
            recentRemovals.clear();
            removalsQueue.clear();

            ok = true;

         } finally {
            removalsLock.unlock();
         }
      }
      catch (Exception e) {
         ok = false;
      }
      finally {
         earliestRemovalTimestamp = invalidationTimestamp;
      }

      return ok;
	}

   /**
    * Notifies this validator that it is expected that a database read followed by a subsequent {@link
    * #acquirePutFromLoadLock(Object)} call will occur. The intent is this method would be called following a cache miss
    * wherein it is expected that a database read plus cache put will occur. Calling this method allows the validator to
    * treat the subsequent <code>acquirePutFromLoadLock</code> as if the database read occurred when this method was
    * invoked. This allows the validator to compare the timestamp of this call against the timestamp of subsequent removal
    * notifications. A put that occurs without this call preceding it is "naked"; i.e the validator must assume the put is
    * not valid if any relevant removal has occurred within {@link #NAKED_PUT_INVALIDATION_PERIOD} milliseconds.
    *
    * @param key key that will be used for subsequent cache put
    */
   public void registerPendingPut(Object key) {
      PendingPut pendingPut = new PendingPut(key, getOwnerForPut());
      PendingPutMap pendingForKey = new PendingPutMap(pendingPut);

      for (;;) {
         PendingPutMap existing = pendingPuts.putIfAbsent(key, pendingForKey);
         if (existing != null) {
            if (existing.acquireLock(10, TimeUnit.SECONDS)) {
               try {
                  existing.put(pendingPut);
                  PendingPutMap doublecheck = pendingPuts.putIfAbsent(key, existing);
                  if (doublecheck == null || doublecheck == existing) {
                     break;
                  }
                  // else we hit a race and need to loop to try again
               }
               finally {
                  existing.releaseLock();
               }
            } else {
               // Can't get the lock; when we come back we'll be a "naked put"
               break;
            }
         } else {
            // normal case
            break;
         }
      }

      // Guard against memory leaks
      preventOutdatedPendingPuts(pendingPut);
   }

   // -------------------------------------------------------------- Protected

   /** Only for use by unit tests; may be removed at any time */
   protected int getPendingPutQueueLength() {
      pendingLock.lock();
      try {
         return pendingQueue.size();
      } finally {
         pendingLock.unlock();
      }
   }

   /** Only for use by unit tests; may be removed at any time */
   protected int getOveragePendingPutQueueLength() {
      pendingLock.lock();
      try {
         return overagePendingQueue.size();
      } finally {
         pendingLock.unlock();
      }
   }

   /** Only for use by unit tests; may be removed at any time */
   protected int getRemovalQueueLength() {
      removalsLock.lock();
      try {
         return removalsQueue.size();
      } finally {
         removalsLock.unlock();
      }
   }

   // ---------------------------------------------------------------- Private

   private Object getOwnerForPut() {
      Transaction tx = null;
      try {
         if (transactionManager != null) {
            tx = transactionManager.getTransaction();
         }
      } catch (SystemException se) {
         throw new CacheException("Could not obtain transaction", se);
      }
      return tx == null ? Thread.currentThread() : tx;

   }

   private void preventOutdatedPendingPuts(PendingPut pendingPut) {
      pendingLock.lock();
      try {
         pendingQueue.add(new WeakReference<PendingPut>(pendingPut));
         if (pendingQueue.size() > 1) {
            cleanOutdatedPendingPuts(pendingPut.timestamp, false);
         }
      } finally {
         pendingLock.unlock();
      }
   }

   private void cleanOutdatedPendingPuts(long now, boolean lock) {

      PendingPut toClean = null;
      if (lock) {
         pendingLock.lock();
      }
      try {
         // Clean items out of the basic queue
         long overaged = now - this.pendingPutOveragePeriod;
         long recent = now - this.pendingPutRecentPeriod;

         int pos = 0;
         while (pendingQueue.size() > pos) {
            WeakReference<PendingPut> ref = pendingQueue.get(pos);
            PendingPut item = ref.get();
            if (item == null || item.completed) {
               pendingQueue.remove(pos);
            } else if (item.timestamp < overaged) {
               // Potential leak; move to the overaged queued
               pendingQueue.remove(pos);
               overagePendingQueue.add(ref);
            } else if (item.timestamp >= recent) {
               // Don't waste time on very recent items
               break;
            } else if (pos > 2) {
               // Don't spend too much time getting nowhere
               break;
            } else {
               // Move on to the next item
               pos++;
            }
         }

         // Process the overage queue until we find an item to clean
         // or an incomplete item that hasn't aged out
         long mustCleanTime = now - this.maxPendingPutDelay;

         while (overagePendingQueue.size() > 0) {
            WeakReference<PendingPut> ref = overagePendingQueue.get(0);
            PendingPut item = ref.get();
            if (item == null || item.completed) {
               overagePendingQueue.remove(0);
            } else {
               if (item.timestamp < mustCleanTime) {
                  overagePendingQueue.remove(0);
                  toClean = item;
               }
               break;
            }
         }
      } finally {
         if (lock) {
            pendingLock.unlock();
         }
      }

      // We've found a pendingPut that never happened; clean it up
      if (toClean != null) {
         PendingPutMap map = pendingPuts.get(toClean.key);
         if (map != null) {
            if (map.acquireLock(100, TimeUnit.MILLISECONDS)) {
               try {
                  PendingPut cleaned = map.remove(toClean.owner);
                  if (toClean.equals(cleaned) == false) {
                     // Oops. Restore it.
                     map.put(cleaned);
                  } else if (map.size() == 0) {
                     pendingPuts.remove(toClean.key);
                  }
               }
               finally {
                  map.releaseLock();
               }
            } else {
               // Something's gone wrong and the lock isn't being released.
               // We removed toClean from the queue and need to restore it
               // TODO this is pretty dodgy
               restorePendingPut(toClean);
            }
         }
      }

   }

   private void restorePendingPut(PendingPut toRestore) {
      pendingLock.lock();
      try {
         // Give it a new lease on life so it's not out of order. We could
         // scan the queue and put toRestore back at the front, but then
         // we'll just immediately try removing it again; instead we
         // let it cycle through the queue again
         toRestore.refresh();
         pendingQueue.add(new WeakReference<PendingPut>(toRestore));
      }
      finally {
         pendingLock.unlock();
      }
   }

   /**
    * Lazy-initialization map for PendingPut. Optimized for the expected usual case where only a
    * single put is pending for a given key.
    * 
    * This class is NOT THREAD SAFE. All operations on it must be performed with the lock held.
    */
   private static class PendingPutMap {
      private PendingPut singlePendingPut;
      private Map<Object, PendingPut> fullMap;
      private final Lock lock = new ReentrantLock();

      PendingPutMap(PendingPut singleItem) {
         this.singlePendingPut = singleItem;
      }

      public void put(PendingPut pendingPut) {
         if (singlePendingPut == null) {
            if (fullMap == null) {
               // initial put
               singlePendingPut = pendingPut;
            } else {
               fullMap.put(pendingPut.owner, pendingPut);
            }
         } else {
            // 2nd put; need a map
            fullMap = new HashMap<Object, PendingPut>(4);
            fullMap.put(singlePendingPut.owner, singlePendingPut);
            singlePendingPut = null;
            fullMap.put(pendingPut.owner, pendingPut);
         }
      }

      public PendingPut remove(Object ownerForPut) {
         PendingPut removed = null;
         if (fullMap == null) {
            if (singlePendingPut != null
                  && singlePendingPut.owner.equals(ownerForPut)) {
               removed = singlePendingPut;
               singlePendingPut = null;
            }
         } else {
            removed = fullMap.remove(ownerForPut);
         }
         return removed;
      }

      public int size() {
         return fullMap == null ? (singlePendingPut == null ? 0 : 1)
               : fullMap.size();
      }

      public boolean acquireLock(long time, TimeUnit unit) {
         try {
            return lock.tryLock(time, unit);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
         }
      }

      public void releaseLock() {
         lock.unlock();
      }

      public void invalidate() {
         if (singlePendingPut != null) {
            singlePendingPut.completed = true;
         } else if (fullMap != null) {
            for (PendingPut pp : fullMap.values()) {
               pp.completed = true;
            }
         }
      }
   }

   private static class PendingPut {
      private final Object key;
      private final Object owner;
      private long timestamp = System.currentTimeMillis();
      private volatile boolean completed;

      private PendingPut(Object key, Object owner) {
         this.key = key;
         this.owner = owner;
      }

      private void refresh() {
         timestamp = System.currentTimeMillis();
      }
   }

   private static class RecentRemoval {
      private final Object key;
      private final Long timestamp;

      private RecentRemoval(Object key, long nakedPutInvalidationPeriod) {
         this.key = key;
         timestamp = Long.valueOf(System.currentTimeMillis() + nakedPutInvalidationPeriod);
      }
   }

}

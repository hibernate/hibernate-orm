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
package org.hibernate.cache.jbc.access;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
 *
 * @author Brian Stansberry
 * 
 * @version $Revision: $
 */
public class PutFromLoadValidator {
	/**
	 * Period in ms after a removal during which a call to
	 * {@link #isPutValid(Object)} that hasn't been
	 * {@link #registerPendingPut(Object) pre-registered} (aka a "naked put")
	 * will return false.
	 */
	public static final long NAKED_PUT_INVALIDATION_PERIOD = 10 * 1000;

	/** Period after which a pending put is placed in the over-age queue */
	private static final long PENDING_PUT_OVERAGE_PERIOD = 5 * 1000;

	/** Period before which we stop trying to clean out pending puts */
	private static final long PENDING_PUT_RECENT_PERIOD = 2 * 1000;

	/**
	 * Period after which a pending put is never expected to come in and should
	 * be cleaned
	 */
	private static final long MAX_PENDING_PUT_DELAY = 2 * 60 * 1000;

	/**
	 * Used to determine whether the owner of a pending put is a thread or a
	 * transaction
	 */
	private final TransactionManager transactionManager;

	private final long nakedPutInvalidationPeriod;
	private final long pendingPutOveragePeriod;
	private final long pendingPutRecentPeriod;
	private final long maxPendingPutDelay;

	/**
	 * Registry of expected, future, isPutValid calls. If a key+owner is
	 * registered in this map, it is not a "naked put" and is allowed to
	 * proceed.
	 */
	private final ConcurrentMap<Object, PendingPutMap> pendingPuts = new ConcurrentHashMap<Object, PendingPutMap>();
	/**
	 * List of pending puts. Used to ensure we don't leak memory via the
	 * pendingPuts map
	 */
	private final List<WeakReference<PendingPut>> pendingQueue = new LinkedList<WeakReference<PendingPut>>();
	/**
	 * Separate list of pending puts that haven't been resolved within
	 * PENDING_PUT_OVERAGE_PERIOD. Used to ensure we don't leak memory via the
	 * pendingPuts map. Tracked separately from more recent pending puts for
	 * efficiency reasons.
	 */
	private final List<WeakReference<PendingPut>> overagePendingQueue = new LinkedList<WeakReference<PendingPut>>();
	/** Lock controlling access to pending put queues */
	private final Lock pendingLock = new ReentrantLock();
	private final ConcurrentMap<Object, Long> recentRemovals = new ConcurrentHashMap<Object, Long>();
	/**
	 * List of recent removals. Used to ensure we don't leak memory via the
	 * recentRemovals map
	 */
	private final List<RecentRemoval> removalsQueue = new LinkedList<RecentRemoval>();
	/**
	 * The time when the first element in removalsQueue will expire. No reason
	 * to do housekeeping on the queue before this time.
	 */
	private volatile long earliestRemovalTimestamp;
	/** Lock controlling access to removalsQueue */
	private final Lock removalsLock = new ReentrantLock();

	/**
	 * The time of the last call to regionRemoved(), plus
	 * NAKED_PUT_INVALIDATION_PERIOD. All naked puts will be rejected until the
	 * current time is greater than this value.
	 */
	private volatile long invalidationTimestamp;

	/**
	 * Creates a new PutFromLoadValidator.
	 * 
	 * @param transactionManager
	 *            transaction manager to use to associated changes with a
	 *            transaction; may be <code>null</code>
	 */
	public PutFromLoadValidator(TransactionManager transactionManager) {
		this(transactionManager, NAKED_PUT_INVALIDATION_PERIOD,
				PENDING_PUT_OVERAGE_PERIOD, PENDING_PUT_RECENT_PERIOD,
				MAX_PENDING_PUT_DELAY);
	}

	/**
	 * Constructor variant for use by unit tests; allows control of various
	 * timeouts by the test.
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

	public boolean isPutValid(Object key) {
		boolean valid = false;
		long now = System.currentTimeMillis();

		PendingPutMap pending = pendingPuts.get(key);
		if (pending != null) {
			synchronized (pending) {
				PendingPut toCancel = pending.remove(getOwnerForPut());
				valid = toCancel != null;
				if (valid) {
					toCancel.completed = true;
					if (pending.size() == 0) {
						pendingPuts.remove(key);
					}
				}
			}
		}

		if (!valid) {
			if (now > invalidationTimestamp) {
				Long removedTime = recentRemovals.get(key);
				if (removedTime == null || now > removedTime.longValue()) {
					valid = true;
				}
			}
		}

		cleanOutdatedPendingPuts(now, true);

		return valid;
	}

	public void keyRemoved(Object key) {
		// Invalidate any pending puts
		pendingPuts.remove(key);

		// Record when this occurred to invalidate later naked puts
		RecentRemoval removal = new RecentRemoval(key,
				this.nakedPutInvalidationPeriod);
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
				earliestRemovalTimestamp = removalsQueue.get(0).timestamp
						.longValue();
			}
		} finally {
			removalsLock.unlock();
		}

		if (toClean != null) {
			Long cleaned = recentRemovals.get(toClean.key);
			if (cleaned != null && cleaned.equals(toClean.timestamp)) {
				cleaned = recentRemovals.remove(toClean.key);
				if (cleaned != null
						&& cleaned.equals(toClean.timestamp) == false) {
					// Oops; removed the wrong timestamp; restore it
					recentRemovals.putIfAbsent(toClean.key, cleaned);
				}
			}
		}
	}

	public void regionRemoved() {
		invalidationTimestamp = System.currentTimeMillis()
				+ this.nakedPutInvalidationPeriod;
		pendingLock.lock();
		try {
			removalsLock.lock();
			try {
				pendingPuts.clear();
				pendingQueue.clear();
				overagePendingQueue.clear();
				recentRemovals.clear();
				removalsQueue.clear();
				earliestRemovalTimestamp = invalidationTimestamp;

			} finally {
				removalsLock.unlock();
			}
		} finally {
			pendingLock.unlock();
		}
	}

	/**
	 * Notifies this validator that it is expected that a database read followed
	 * by a subsequent {@link #isPutValid(Object)} call will occur. The intent
	 * is this method would be called following a cache miss wherein it is
	 * expected that a database read plus cache put will occur. Calling this
	 * method allows the validator to treat the subsequent
	 * <code>isPutValid</code> as if the database read occurred when this method
	 * was invoked. This allows the validator to compare the timestamp of this
	 * call against the timestamp of subsequent removal notifications. A put
	 * that occurs without this call preceding it is "naked"; i.e the validator
	 * must assume the put is not valid if any relevant removal has occurred
	 * within {@link #NAKED_PUT_INVALIDATION_PERIOD} milliseconds.
	 * 
	 * @param key
	 *            key that will be used for subsequent put
	 */
	public void registerPendingPut(Object key) {
		PendingPut pendingPut = new PendingPut(key, getOwnerForPut());
		PendingPutMap pendingForKey = new PendingPutMap();
		synchronized (pendingForKey) {
			for (;;) {
				PendingPutMap existing = pendingPuts.putIfAbsent(key,
						pendingForKey);
				if (existing != null && existing != pendingForKey) {
					synchronized (existing) {
						existing.put(pendingPut);
						PendingPutMap doublecheck = pendingPuts.putIfAbsent(
								key, existing);
						if (doublecheck == null || doublecheck == existing) {
							break;
						}
						// else we hit a race and need to loop to try again
					}
				} else {
					pendingForKey.put(pendingPut);
					break;
				}
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
			cleanOutdatedPendingPuts(pendingPut.timestamp, false);
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
				synchronized (map) {
					PendingPut cleaned = map.remove(toClean.owner);
					if (toClean.equals(cleaned) == false) {
						// Oops. Restore it.
						map.put(cleaned);
					} else if (map.size() == 0) {
						pendingPuts.remove(toClean.key);
					}
				}
			}
		}

	}

	/**
	 * Lazy-initialization map for PendingPut. Optimized for the expected usual
	 * case where only a single put is pending for a given key.
	 * 
	 * This class is NOT THREAD SAFE. All operations on it must be performed
	 * with the object monitor held.
	 */
	private static class PendingPutMap {
		private PendingPut singlePendingPut;
		private Map<Object, PendingPut> fullMap;

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
	}

	private static class PendingPut {
		private final Object key;
		private final Object owner;
		private final long timestamp = System.currentTimeMillis();
		private volatile boolean completed;

		private PendingPut(Object key, Object owner) {
			this.key = key;
			this.owner = owner;
		}

	}

	private static class RecentRemoval {
		private final Object key;
		private final Long timestamp;

		private RecentRemoval(Object key, long nakedPutInvalidationPeriod) {
			this.key = key;
			timestamp = Long.valueOf(System.currentTimeMillis()
					+ nakedPutInvalidationPeriod);
		}
	}

}

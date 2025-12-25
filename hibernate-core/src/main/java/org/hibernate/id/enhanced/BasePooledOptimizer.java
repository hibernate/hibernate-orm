/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base class for optimizers that use a pool of values.
 * This class exists to support the requirements of
 * {@code JtaCompensatingIsolationDelegate} when the
 * {@code JtaPlatform} does not let us suspend the
 * current transaction to increment the database value
 * underlying a table generator. Note that this problem
 * is not relevant to sequence generation because
 * sequence incrementation is non-transactional.
 * <p>
 * The worst that can happen here is that we have a
 * bunch of incoming requests which each wait for 0.2s
 * before falling back to retrieving an id from the
 * database. That is, in the worst case we have the same
 * performance as a table generator with an allocation
 * size of 1, with a slight additional delay.
 *
 * @author Gavin King
 * @since 7.3
 */
@Incubating
public abstract class BasePooledOptimizer extends AbstractOptimizer {

	/**
	 * The maximum time we wait allowing another transaction to
	 * finish and share its allocated id block with us, before
	 * we break its lock and allocate a new block of our own.
	 */
	private static final int WAIT_MILLIS = 200;

	private ReentrantLock longLock = new ReentrantLock();

	/**
	 * @param returnClass The expected id class.
	 * @param incrementSize The increment size
	 */
	public BasePooledOptimizer(Class<?> returnClass, int incrementSize) {
		super( returnClass, incrementSize );
	}

	@Override
	public synchronized void begin(AccessCallback callback) {
		try {
			// wait for a maximum of 0.2s for other lockers
			// to finish up their work, so we don't have to
			// duplicate their efforts
			if ( !longLock.tryLock( WAIT_MILLIS, TimeUnit.MILLISECONDS ) ) {
				// the last locker did not clean up after itself
				// or is still working and blocking everyone else,
				// and so we're going to break their lock, in a
				// way which also signals to them that we broke it
				longLock = new ReentrantLock();
				longLock.lock();
				// we can't know that the last increment succeeded,
				// so we will have to go back to the database
				// (potentially wasting some already-allocated ids)
				// we need to throw away the work the other guy did
				reset();
			}
		}
		catch (InterruptedException e) {
			throw new HibernateException( "Interrupted while waiting for lock", e );
		}
	}

	@Override
	public synchronized void commit(AccessCallback callback) {
		// our containing transaction completed successfully
		if ( longLock.isHeldByCurrentThread() ) {
			// our containing transaction succeeded, and our
			// lock was not broken, so the work we did can now
			// be safely shared with everyone else
			longLock.unlock();
		}
	}

	@Override
	public synchronized void rollback(AccessCallback callback) {
		// our containing transaction failed
		if ( longLock.isHeldByCurrentThread() ) {
			// nobody else has had a chance to attempt to recover
			// from our failure, so the current state reflects
			// work we did that has to now be discarded, and
			// the next guy will have to try again in his tx
			reset();
			longLock.unlock();
		}
	}
}

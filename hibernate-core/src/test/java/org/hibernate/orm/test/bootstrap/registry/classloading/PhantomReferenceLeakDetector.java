/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.registry.classloading;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.Assert;

/**
 * Utility to help verify that a certain object is free
 * to be garbage collected (that we're not leaking it).
 * This is particularly useful with Classloaders.
 *
 * @author Sanne Grinovero (C) 2023 Red Hat Inc.
 */
public final class PhantomReferenceLeakDetector {

	/**
	 * A single second should be more than enough; this might need
	 * to be tuned for particularly slow systems to avoid
	 * flaky tests, but I personally believe it's very
	 * large: we default to a very generous amount as we won't
	 * normally wait for this long, unless there is a problem.
	 * So consider carefully if there's not a deeper
	 * problem before setting these to even larger amounts.
	 */
	private static final int MAX_TOTAL_WAIT_SECONDS = 180;
	private static final int GC_ATTEMPTS = MAX_TOTAL_WAIT_SECONDS * 5;
	private static PhantomReference hold;

	/**
	 * Asserts that a certain operation won't be leaking
	 * a particular object of type T.
	 * The operation being tested needs to implement {@link Supplier} and
	 * return a reference to the object to monitor; we expect
	 * this object to be eligible for garbage collection soon after the
	 * action is completed, and therefore great care must be taken
	 * for the test itself to not leak a reference to such object
	 * either, including on the caller's stack; this implies it
	 * might be necessary to explicitly null local variables
	 * if the test infrastructure is referring to the critical object.
	 * For an object to not be considered leaked, it must be
	 * garbage collected in a reasonable time after the action; since
	 * we rely on the GC operation, which is asynchronous and not deterministic,
	 * it's possible that this test could fail even without a real leak;
	 * to prevent flaky tests we use a very generously sized timeout and
	 * we might trigger multiple GC events.
	 * This approach implies that a failing assertion might not necessarily
	 * signal that there definitively is a leak, but the test not failing
	 * should imply we're definitively fine.
	 * If a test using this utility were to suddenly start failing
	 * beware of raising the timeouts without investigating: if the object
	 * is eventually garbage collected but taking an unusual amount of time,
	 * that's also a sign of something not being quite right.
	 */
	public static <T> void assertActionNotLeaking(Supplier<T> action) {
		Assert.assertTrue("Operation apparently leaked the critical resource",
						verifyActionNotLeaking( action )
		);
	}

	static <T> boolean verifyActionNotLeaking(Supplier<T> action) {
		return verifyActionNotLeaking( action, GC_ATTEMPTS, MAX_TOTAL_WAIT_SECONDS );
	}

	/**
	 * Exposed for self-testing w/o having to wait for the regular timeout
	 */
	static <T> boolean verifyActionNotLeaking(Supplier<T> action, final int gcAttempts, final int totalWaitSeconds) {
		T criticalReference = action.get();
		final ReferenceQueue<T> referenceQueue = new ReferenceQueue<>();
		final PhantomReference<T> reference = new PhantomReference<>( criticalReference, referenceQueue );
		holdOnTo( reference );
		//Ignore IDE's suggestion to remove the following line: we really need it!
		// (it could be inlined above, but I prefer this style so that it serves as an example for
		// future maintenance of how this works)
		criticalReference = null;
		try {
			return verifyCollection( referenceQueue, gcAttempts, totalWaitSeconds );
		}
		finally {
			clearHeldReferences();
		}
	}

	private static synchronized void clearHeldReferences() {
		PhantomReferenceLeakDetector.hold = null;
	}

	/**
	 * Ensure we keep a reference to the PhantomReference until we're done
	 * with the test, otherwise it might get collected before having had
	 * a change to trigger; this seems to behave differently on different
	 * JVMs but is most likely just a timing issue.
	 * @param reference
	 */
	private static synchronized void holdOnTo(PhantomReference reference) {
		if ( PhantomReferenceLeakDetector.hold != null ) {
			throw new IllegalStateException( "Detected recursive use of PhantomReferenceLeakDetector, which is not supported, or possibly a leaked previous run" );
		}
		PhantomReferenceLeakDetector.hold = reference;
	}

	private static <T> boolean verifyCollection(final ReferenceQueue<T> referenceQueue, final int gcAttempts, final int totalWaitSeconds) {
		final int millisEachAttempt = Math.round((float) TimeUnit.SECONDS.toMillis( totalWaitSeconds ) / gcAttempts );
		for ( int i = 0; i < gcAttempts; i++ ) {
			Runtime.getRuntime().gc();
			try {
				Reference<?> ref = referenceQueue.remove( millisEachAttempt );
				if ( ref != null ) {
					return true;
				}
			}
			catch ( InterruptedException e ) {
				//let's try another GC: if there's complex finalizers on the path to the object
				//that needs to be tested a single GC cycle might not be enough for it to get collected.
				//(We don't expect any complex finalizer so we won't be waiting too much either..)
			}
		}
		return false;
	}

}

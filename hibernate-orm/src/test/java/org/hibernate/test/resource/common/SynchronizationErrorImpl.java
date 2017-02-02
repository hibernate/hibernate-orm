/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.resource.common;

import javax.transaction.Synchronization;

/**
 * @author Steve Ebersole
 */
public class SynchronizationErrorImpl implements Synchronization {
	private final boolean errorOnBefore;
	private final boolean errorOnAfter;

	public static SynchronizationErrorImpl forBefore() {
		return new SynchronizationErrorImpl( true, false );
	}

	public static SynchronizationErrorImpl forAfter() {
		return new SynchronizationErrorImpl( false, true );
	}

	public static SynchronizationErrorImpl forBoth() {
		return new SynchronizationErrorImpl( true, true );
	}

	private SynchronizationErrorImpl(boolean errorOnBefore, boolean errorOnAfter) {
		this.errorOnBefore = errorOnBefore;
		this.errorOnAfter = errorOnAfter;
	}

	@Override
	public void beforeCompletion() {
		if ( errorOnBefore ) {
			throw new RuntimeException( "throwing requested error on beforeCompletion" );
		}
	}

	@Override
	public void afterCompletion(int status) {
		if ( errorOnAfter ) {
			throw new RuntimeException( "throwing requested error on afterCompletion" );
		}
	}
}

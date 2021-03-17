/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import org.hibernate.SessionEventListener;
import org.hibernate.engine.spi.SessionEventListenerManager;

/**
 * @author Steve Ebersole
 */
public class SessionEventListenerManagerImpl implements SessionEventListenerManager, Serializable {

	private SessionEventListener[] listeners;

	public SessionEventListenerManagerImpl(SessionEventListener... initialListener) {
		//no need for defensive copies until the array is mutated:
		this.listeners = initialListener;
	}

	@Override
	public void addListener(final SessionEventListener... additionalListeners) {
		Objects.requireNonNull( additionalListeners );
		final SessionEventListener[] existing = this.listeners;
		if ( existing == null ) {
			//Make a defensive copy as this array can be tracked back to API (user code)
			this.listeners = Arrays.copyOf( additionalListeners, additionalListeners.length );
		}
		else {
			// Resize our existing array and add the new listeners
			final SessionEventListener[] newlist = new SessionEventListener[ existing.length + additionalListeners.length ];
			System.arraycopy( existing, 0, newlist, 0, existing.length );
			System.arraycopy( additionalListeners, 0, newlist, existing.length, additionalListeners.length );
			this.listeners = newlist;
		}
	}

	@Override
	public void transactionCompletion(boolean successful) {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.transactionCompletion( successful );
		}
	}

	@Override
	public void jdbcConnectionAcquisitionStart() {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.jdbcConnectionAcquisitionStart();
		}
	}

	@Override
	public void jdbcConnectionAcquisitionEnd() {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.jdbcConnectionAcquisitionEnd();
		}
	}

	@Override
	public void jdbcConnectionReleaseStart() {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.jdbcConnectionReleaseStart();
		}
	}

	@Override
	public void jdbcConnectionReleaseEnd() {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.jdbcConnectionReleaseEnd();
		}
	}

	@Override
	public void jdbcPrepareStatementStart() {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.jdbcPrepareStatementStart();
		}
	}

	@Override
	public void jdbcPrepareStatementEnd() {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.jdbcPrepareStatementEnd();
		}
	}

	@Override
	public void jdbcExecuteStatementStart() {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.jdbcExecuteStatementStart();
		}
	}

	@Override
	public void jdbcExecuteStatementEnd() {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.jdbcExecuteStatementEnd();
		}
	}

	@Override
	public void jdbcExecuteBatchStart() {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.jdbcExecuteBatchStart();
		}
	}

	@Override
	public void jdbcExecuteBatchEnd() {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.jdbcExecuteBatchEnd();
		}
	}

	@Override
	public void cachePutStart() {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.cachePutStart();
		}
	}

	@Override
	public void cachePutEnd() {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.cachePutEnd();
		}
	}

	@Override
	public void cacheGetStart() {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.cacheGetStart();
		}
	}

	@Override
	public void cacheGetEnd(boolean hit) {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.cacheGetEnd( hit );
		}
	}

	@Override
	public void flushStart() {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.flushStart();
		}
	}

	@Override
	public void flushEnd(int numberOfEntities, int numberOfCollections) {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.flushEnd( numberOfEntities, numberOfCollections );
		}
	}

	@Override
	public void partialFlushStart() {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.partialFlushStart();
		}
	}

	@Override
	public void partialFlushEnd(int numberOfEntities, int numberOfCollections) {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.partialFlushEnd( numberOfEntities, numberOfCollections );
		}
	}

	@Override
	public void dirtyCalculationStart() {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.dirtyCalculationStart();
		}
	}

	@Override
	public void dirtyCalculationEnd(boolean dirty) {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.dirtyCalculationEnd( dirty );
		}
	}

	@Override
	public void end() {
		if ( listeners == null ) {
			return;
		}

		for ( SessionEventListener listener : listeners ) {
			listener.end();
		}
	}
}

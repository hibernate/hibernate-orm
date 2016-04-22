/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionEventListener;
import org.hibernate.engine.spi.SessionEventListenerManager;

/**
 * @author Steve Ebersole
 */
public class SessionEventListenerManagerImpl implements SessionEventListenerManager, Serializable {
	private List<SessionEventListener> listenerList;

	@Override
	public void addListener(SessionEventListener... listeners) {
		if ( listenerList == null ) {
			listenerList = new ArrayList<>();
		}

		java.util.Collections.addAll( listenerList, listeners );
	}

	@Override
	public void transactionCompletion(boolean successful) {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.transactionCompletion( successful );
		}
	}

	@Override
	public void jdbcConnectionAcquisitionStart() {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.jdbcConnectionAcquisitionStart();
		}
	}

	@Override
	public void jdbcConnectionAcquisitionEnd() {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.jdbcConnectionAcquisitionEnd();
		}
	}

	@Override
	public void jdbcConnectionReleaseStart() {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.jdbcConnectionReleaseStart();
		}
	}

	@Override
	public void jdbcConnectionReleaseEnd() {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.jdbcConnectionReleaseEnd();
		}
	}

	@Override
	public void jdbcPrepareStatementStart() {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.jdbcPrepareStatementStart();
		}
	}

	@Override
	public void jdbcPrepareStatementEnd() {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.jdbcPrepareStatementEnd();
		}
	}

	@Override
	public void jdbcExecuteStatementStart() {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.jdbcExecuteStatementStart();
		}
	}

	@Override
	public void jdbcExecuteStatementEnd() {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.jdbcExecuteStatementEnd();
		}
	}

	@Override
	public void jdbcExecuteBatchStart() {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.jdbcExecuteBatchStart();
		}
	}

	@Override
	public void jdbcExecuteBatchEnd() {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.jdbcExecuteBatchEnd();
		}
	}

	@Override
	public void cachePutStart() {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.cachePutStart();
		}
	}

	@Override
	public void cachePutEnd() {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.cachePutEnd();
		}
	}

	@Override
	public void cacheGetStart() {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.cacheGetStart();
		}
	}

	@Override
	public void cacheGetEnd(boolean hit) {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.cacheGetEnd( hit );
		}
	}

	@Override
	public void flushStart() {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.flushStart();
		}
	}

	@Override
	public void flushEnd(int numberOfEntities, int numberOfCollections) {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.flushEnd( numberOfEntities, numberOfCollections );
		}
	}

	@Override
	public void partialFlushStart() {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.partialFlushStart();
		}
	}

	@Override
	public void partialFlushEnd(int numberOfEntities, int numberOfCollections) {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.partialFlushEnd( numberOfEntities, numberOfCollections );
		}
	}

	@Override
	public void dirtyCalculationStart() {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.dirtyCalculationStart();
		}
	}

	@Override
	public void dirtyCalculationEnd(boolean dirty) {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.dirtyCalculationEnd( dirty );
		}
	}

	@Override
	public void end() {
		if ( listenerList == null ) {
			return;
		}

		for ( SessionEventListener listener : listenerList ) {
			listener.end();
		}
	}
}

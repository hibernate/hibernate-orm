/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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

	public void addListener(SessionEventListener... listeners) {
		if ( listenerList == null ) {
			listenerList = new ArrayList<SessionEventListener>();
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

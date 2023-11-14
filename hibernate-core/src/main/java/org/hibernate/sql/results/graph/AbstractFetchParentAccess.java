/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * Base support for FetchParentAccess implementations.  Mainly adds support for
 * registering and managing resolution listeners
 */
public abstract class AbstractFetchParentAccess implements FetchParentAccess {
	private List<Consumer<Object>> listeners;
	private boolean parentShallowCached;

	@Override
	public void registerResolutionListener(Consumer<Object> listener) {
		if ( listeners == null ) {
			listeners = new ArrayList<>();
		}

		listeners.add( listener );
	}

	protected void clearResolutionListeners() {
		if ( listeners != null ) {
			listeners.clear();
		}
	}

	protected void notifyResolutionListeners(Object resolvedInstance) {
		if ( listeners == null ) {
			return;
		}

		for ( Consumer<Object> listener : listeners ) {
			listener.accept( resolvedInstance );
		}

		listeners.clear();
	}

	protected boolean isParentShallowCached() {
		return parentShallowCached;
	}

	@Override
	public void markShallowCached() {
		parentShallowCached = true;
	}

	@Override
	public void endLoading(ExecutionContext executionContext) {
		parentShallowCached = false;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractFetchParentAccess implements FetchParentAccess {
	private List<Consumer<Object>> listeners;

	@Override
	public void registerResolutionListener(Consumer<Object> listener) {
		if ( listeners == null ) {
			listeners = new ArrayList<>();
		}

		listeners.add( listener );
	}

	protected void clearParentResolutionListeners() {
		if ( listeners != null ) {
			listeners.clear();
		}
	}

	protected void notifyParentResolutionListeners(Object parentInstance) {
		if ( listeners == null ) {
			return;
		}

		for ( Consumer<Object> listener : listeners ) {
			listener.accept( parentInstance );
		}
	}
}

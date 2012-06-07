/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;

/**
 * @author Steve Ebersole
 */
public class SessionFactoryObserverChain implements SessionFactoryObserver {
	private List<SessionFactoryObserver> observers;

	public void addObserver(SessionFactoryObserver observer) {
		if ( observers == null ) {
			observers = new ArrayList<SessionFactoryObserver>();
		}
		observers.add( observer );
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		if ( observers == null ) {
			return;
		}

		for ( SessionFactoryObserver observer : observers ) {
			observer.sessionFactoryCreated( factory );
		}
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		if ( observers == null ) {
			return;
		}

		//notify in reverse order of create notification
		int size = observers.size();
		for (int index = size - 1 ; index >= 0 ; index--) {
			observers.get( index ).sessionFactoryClosed( factory );
		}
	}
}

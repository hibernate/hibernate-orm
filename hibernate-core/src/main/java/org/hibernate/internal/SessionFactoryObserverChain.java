/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
			observers = new ArrayList<>();
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
	public void sessionFactoryClosing(SessionFactory factory) {
		if ( observers == null ) {
			return;
		}

		//notify in reverse order of create notification
		int size = observers.size();
		for (int index = size - 1 ; index >= 0 ; index--) {
			observers.get( index ).sessionFactoryClosing( factory );
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

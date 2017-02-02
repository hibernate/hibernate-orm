/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.internal.jpa;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.jpa.event.spi.jpa.Callback;
import org.hibernate.jpa.event.spi.jpa.CallbackType;
import org.hibernate.jpa.event.spi.jpa.Listener;

/**
 * Represents a JPA callback using a dedicated listener
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @author Steve Ebersole
 */
public class ListenerCallback extends AbstractCallback implements Callback {
	private final Method callbackMethod;
	private final Listener listenerInstance;

	public ListenerCallback(Listener listenerInstance, Method callbackMethod, CallbackType callbackType) {
		super( callbackType );
		this.listenerInstance = listenerInstance;
		this.callbackMethod = callbackMethod;
	}

	@Override
	public boolean performCallback(Object entity) {
		try {
			callbackMethod.invoke( listenerInstance.getListener(), entity );
			return true;
		}
		catch (InvocationTargetException e) {
			//keep runtime exceptions as is
			if ( e.getTargetException() instanceof RuntimeException ) {
				throw (RuntimeException) e.getTargetException();
			}
			else {
				throw new RuntimeException( e.getTargetException() );
			}
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}
}

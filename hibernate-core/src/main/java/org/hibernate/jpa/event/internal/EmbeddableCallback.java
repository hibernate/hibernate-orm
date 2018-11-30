/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.event.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.property.access.spi.Getter;

/**
 * Represents a JPA callback on the embeddable type
 *
 * @author Vlad Mihalcea
 */
final class EmbeddableCallback extends AbstractCallback {

	private final Getter embeddableGetter;
	private final Method callbackMethod;

	EmbeddableCallback(Getter embeddableGetter, Method callbackMethod, CallbackType callbackType) {
		super( callbackType );
		this.embeddableGetter = embeddableGetter;
		this.callbackMethod = callbackMethod;
	}

	@Override
	public boolean performCallback(Object entity) {
		try {
			Object embeddable = embeddableGetter.get( entity );
			if ( embeddable != null ) {
				callbackMethod.invoke( embeddable );
			}
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

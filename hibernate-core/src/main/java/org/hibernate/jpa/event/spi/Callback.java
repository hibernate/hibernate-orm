/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.event.spi;

import java.io.Serializable;

/**
 * Represents a JPA event callback (the method).
 *
 * Generally there are 2 flavors of this; either an annotated method on the entity itself
 * or an annotated method on a separate "listener" class.  This contract presents
 * a unified abstraction for both cases
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @author Steve Ebersole
 */
public interface Callback extends Serializable {
	/**
	 * The type of callback (pre-update, pre-persist, etc) handled
	 */
	CallbackType getCallbackType();

	/**
	 * Contract for performing the callback
	 *
	 * @param entity Reference to the entity for which the callback is triggered.
	 *
	 * @return Did a callback actually happen?
	 */
	boolean performCallback(Object entity);
}

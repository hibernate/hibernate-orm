/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.spi.jpa;

import java.io.Serializable;

/**
 * Represents a JPA event callback (the method).
 * <p/>
 * Generally there are 2 flavors of this; either an annotated method on the entity itself
 * or an annotated method on a separate "listener" class.  This contract unifies both of
 * these cases.
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @author Steve Ebersole
 */
public interface Callback extends Serializable {
	/**
	 * Is this callback active (will it do anything)?
	 *
	 * @return {@code true} if the callback is active, {@code false} otherwise.
	 *
	 * @deprecated I can actually find no usages of this method and have no idea
	 * why it is here :)
	 */
	@Deprecated
	boolean isActive();

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

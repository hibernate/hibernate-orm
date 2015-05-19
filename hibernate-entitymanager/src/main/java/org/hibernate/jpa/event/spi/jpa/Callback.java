/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.spi.jpa;

import java.io.Serializable;

/**
 * Represents a JPA event callback.
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @author Steve Ebersole
 */
public interface Callback extends Serializable {
	/**
	 * Contract for performing the callback
	 *
	 * @param entity Reference to the entity for which the callback is triggered.
	 *
	 * @return Did a callback actually happen?
	 */
	public boolean performCallback(Object entity);

	/**
	 * Is this callback active (will it do anything)?
	 *
	 * @return
	 */
	public boolean isActive();
}

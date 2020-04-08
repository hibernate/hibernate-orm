/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.engine.spi;

/**
 * Navigation methods for extra state objects attached to {@link org.hibernate.engine.spi.EntityEntry}.
 *
 * @author <a href="mailto:emmanuel@hibernate.org">Emmanuel Bernard</a>
 */
public interface EntityEntryExtraState {

	/**
	 * Attach additional state to the core state of {@link org.hibernate.engine.spi.EntityEntry}
	 * <p>
	 * Implementations must delegate to the next state or add it as next state if last in line.
	 */
	void addExtraState(EntityEntryExtraState extraState);

	/**
	 * Retrieve additional state by class type or null if no extra state of that type is present.
	 * <p>
	 * Implementations must return self if they match or delegate discovery to the next state in line.
	 */
	<T extends EntityEntryExtraState> T getExtraState(Class<T> extraStateType);

	//a remove method is ugly to define and has not real use case that we found: left out
}

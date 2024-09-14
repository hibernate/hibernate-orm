/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.spi;

/**
 * Registry of Callbacks by entity and type
 *
 * @author Steve Ebersole
 */
public interface CallbackRegistry {
	/**
	 * Do we have any registered callbacks of the given type for the given entity?
	 *
	 * @param entityClass The entity Class to check against
	 * @param callbackType The type of callback to look for
	 *
	 * @return {@code true} indicates there are already registered callbacks of
	 * that type for that class; {@code false} indicates there are not.
	 */
	boolean hasRegisteredCallbacks(Class<?> entityClass, CallbackType callbackType);

	void preCreate(Object entity);
	void postCreate(Object entity);

	boolean preUpdate(Object entity);
	void postUpdate(Object entity);

	void preRemove(Object entity);
	void postRemove(Object entity);

	boolean postLoad(Object entity);

	/**
	 * Signals that the CallbackRegistry will no longer be used.
	 * In particular, it is important to release references to class types
	 * to avoid classloader leaks.
	 */
	void release();

}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.spi;

/**
 * Registry of JPA entity lifecycle callbacks by entity and type.
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

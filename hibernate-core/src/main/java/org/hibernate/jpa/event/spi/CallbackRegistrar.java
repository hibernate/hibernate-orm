/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.event.spi;

public interface CallbackRegistrar extends CallbackRegistry {

	/**
	 * Register the callback against the given entity.
	 *
	 * @param entityClass The entity Class to register the Callbacks against
	 * @param callbacks The Callbacks to register against the given entity Class
	 */
	void registerCallbacks(Class entityClass, Callback[] callbacks);

}

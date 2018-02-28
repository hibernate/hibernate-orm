/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.event.spi;

import org.hibernate.mapping.Property;

/**
 * Contract for walking an entity hierarchy and building a list of JPA callbacks
 *
 * @author Steve Ebersole
 */
public interface CallbackBuilder {
	/**
	 * Represents the target of JPA callback registrations as part the EntityCallbackBuilder
	 */
	interface CallbackRegistrar extends CallbackRegistry {

		/**
		 * Register the callback against the given entity.
		 *
		 * @param entityClass The entity Class to register the Callbacks against
		 * @param callbacks The Callbacks to register against the given entity Class
		 */
		void registerCallbacks(Class entityClass, Callback[] callbacks);
	}

	void buildCallbacksForEntity(String entityClassName, CallbackRegistrar callbackRegistrar);

	void buildCallbacksForEmbeddable(
			Property embeddableProperty,
			String entityClassName,
			CallbackRegistrar callbackRegistrar);

	void release();
}

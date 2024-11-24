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
 *
 * @deprecated This SPI has never been functional and is no longer used. It will eventually be removed.
 */
@Deprecated
public interface CallbackBuilder {
	/**
	 * Represents the target of JPA callback registrations as part the EntityCallbackBuilder
	 *
	 * @deprecated Use {@link org.hibernate.jpa.event.spi.CallbackRegistrar} instead.
	 */
	@Deprecated
	interface CallbackRegistrar extends org.hibernate.jpa.event.spi.CallbackRegistrar  {
	}

	void buildCallbacksForEntity(Class entityClass, CallbackRegistrar callbackRegistrar);

	void buildCallbacksForEmbeddable(
			Property embeddableProperty,
			Class entityClass,
			CallbackRegistrar callbackRegistrar);

	void release();
}

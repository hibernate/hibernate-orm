/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jpa.spi;

import jakarta.persistence.EntityListenerRegistration;
import org.hibernate.jpa.event.spi.CallbackType;

import java.util.function.Consumer;

/// Jakarta Persistence style callbacks for a particular entity.
///
/// @param <E> The entity type
///
/// @author Steve Ebersole
public interface EntityCallbacks<E> {
	/// Whether there are any callbacks registered for the entity of this type.
	boolean hasRegisteredCallbacks(CallbackType callbackType);

	/// Handle [jakarta.persistence.PreInsert] callbacks.
	<S extends E> boolean preCreate(S entity);

	/// Handle [jakarta.persistence.PostInsert] callbacks.
	<S extends E> boolean postCreate(S entity);

	/// Handle [jakarta.persistence.PreUpdate] callbacks.
	<S extends E> boolean preUpdate(S entity);

	/// Handle [jakarta.persistence.PostUpdate] callbacks.
	<S extends E> boolean postUpdate(S entity);

	/// Handle [jakarta.persistence.PreRemove] callbacks.
	<S extends E> boolean preRemove(S entity);

	/// Handle [jakarta.persistence.PostRemove] callbacks.
	<S extends E> boolean postRemove(S entity);

	/// Handle [jakarta.persistence.PostLoad] callbacks.
	<S extends E> boolean postLoad(S entity);

	/// @see jakarta.persistence.EntityManagerFactory#addListener
	EntityListenerRegistration addListener(CallbackType type, Consumer<? super E> listener);

	/// @see jakarta.persistence.EntityManagerFactory#addListener
	EntityListenerRegistration addListener(CallbackType type, Callback<? super E> callback);
}

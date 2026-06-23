/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jpa.spi;

import jakarta.persistence.EntityListenerRegistration;
import org.hibernate.jpa.event.spi.CallbackType;

import java.util.function.Consumer;
import jakarta.annotation.Nonnull;

/// Jakarta Persistence style callbacks for a particular entity.
///
/// @param <E> The entity type
///
/// @author Steve Ebersole
public interface EntityCallbacks<E> {
	/// Whether there are any callbacks registered for the entity of this type.
	boolean hasRegisteredCallbacks(@Nonnull CallbackType callbackType);

	/// Handle [jakarta.persistence.PrePersist] callbacks.
	<S extends E> boolean preCreate(@Nonnull S entity);

	/// Handle [jakarta.persistence.PostPersist] callbacks.
	<S extends E> boolean postCreate(@Nonnull S entity);

	/// Handle [jakarta.persistence.PreMerge] callbacks.
	<S extends E> boolean preMerge(@Nonnull S entity);

	/// Handle [jakarta.persistence.PreInsert] callbacks.
	<S extends E> boolean preInsert(@Nonnull S entity);

	/// Handle [jakarta.persistence.PostInsert] callbacks.
	<S extends E> boolean postInsert(@Nonnull S entity);

	/// Handle [jakarta.persistence.PreUpdate] callbacks.
	<S extends E> boolean preUpdate(@Nonnull S entity);

	/// Handle [jakarta.persistence.PostUpdate] callbacks.
	<S extends E> boolean postUpdate(@Nonnull S entity);

	/// Handle [jakarta.persistence.PreUpsert] callbacks.
	<S extends E> boolean preUpsert(@Nonnull S entity);

	/// Handle [jakarta.persistence.PostUpsert] callbacks.
	<S extends E> boolean postUpsert(@Nonnull S entity);

	/// Handle [jakarta.persistence.PreRemove] callbacks.
	<S extends E> boolean preRemove(@Nonnull S entity);

	/// Handle [jakarta.persistence.PostRemove] callbacks.
	<S extends E> boolean postRemove(@Nonnull S entity);

	/// Handle [jakarta.persistence.PreDelete] callbacks.
	<S extends E> boolean preDelete(@Nonnull S entity);

	/// Handle [jakarta.persistence.PostDelete] callbacks.
	<S extends E> boolean postDelete(@Nonnull S entity);

	/// Handle [jakarta.persistence.PostLoad] callbacks.
	<S extends E> boolean postLoad(@Nonnull S entity);

	/// @see jakarta.persistence.EntityManagerFactory#addListener
	@Nonnull
	EntityListenerRegistration addListener(@Nonnull CallbackType type, @Nonnull Consumer<? super E> listener);

	/// @see jakarta.persistence.EntityManagerFactory#addListener
	@Nonnull
	EntityListenerRegistration addListener(@Nonnull CallbackType type, @Nonnull Callback<? super E> callback);
}

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
/// @author Steve Ebersole
public interface EntityCallbacks<E> {
	/// Whether there are any callbacks registered for the entity of this type.
	boolean hasRegisteredCallbacks(CallbackType callbackType);

	/// Handle [jakarta.persistence.PreInsert] callbacks.
	boolean preCreate(E entity);

	/// Handle [jakarta.persistence.PostInsert] callbacks.
	boolean postCreate(E entity);

	/// Handle [jakarta.persistence.PreUpdate] callbacks.
	boolean preUpdate(E entity);

	/// Handle [jakarta.persistence.PostUpdate] callbacks.
	boolean postUpdate(E entity);

	/// Handle [jakarta.persistence.PreRemove] callbacks.
	boolean preRemove(E entity);

	/// Handle [jakarta.persistence.PostRemove] callbacks.
	boolean postRemove(E entity);

	/// Handle [jakarta.persistence.PostLoad] callbacks.
	boolean postLoad(E entity);

	/// @see jakarta.persistence.EntityManagerFactory#addListener
	EntityListenerRegistration addListener(CallbackType type, Consumer<E> listener);

	/// @see jakarta.persistence.EntityManagerFactory#addListener
	EntityListenerRegistration addListener(CallbackType type, Callback<E> callback);

	/// Signals that the callbacks will no longer be used.
	/// In particular, it is important to release references to class types
	/// to avoid classloader leaks.
	///
	/// @todo (jpa4) : do we still need this?
	/// 		Was useful when we kept	these in Map keyed by the entity-type,
	/// 		but not so sure it is useful here...
	void release();
}

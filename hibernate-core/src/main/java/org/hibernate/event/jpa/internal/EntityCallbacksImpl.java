/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jpa.internal;

import jakarta.persistence.EntityListenerRegistration;
import org.hibernate.event.jpa.spi.EntityCallbacks;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.event.jpa.spi.Callback;
import org.hibernate.jpa.event.spi.CallbackType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/// Implementation of EntityCallbacks
///
/// @author Steve Ebersole
public class EntityCallbacksImpl<E> implements EntityCallbacks<E>, Serializable {
	private final List<Callback<E>> preCreateCallbacks;
	private final List<Callback<E>> postCreateCallbacks;

	private final List<Callback<E>> preUpdateCallbacks;
	private final List<Callback<E>> postUpdateCallbacks;

	private final List<Callback<E>> preRemoveCallbacks;
	private final List<Callback<E>> postRemoveCallbacks;

	private final List<Callback<E>> postLoadCallbacks;

	private EntityCallbacksImpl(
			List<Callback<E>> preCreateCallbacks,
			List<Callback<E>> postCreateCallbacks,
			List<Callback<E>> preUpdateCallbacks,
			List<Callback<E>> postUpdateCallbacks,
			List<Callback<E>> preRemoveCallbacks,
			List<Callback<E>> postRemoveCallbacks,
			List<Callback<E>> postLoadCallbacks) {
		this.preCreateCallbacks = preCreateCallbacks;
		this.postCreateCallbacks = postCreateCallbacks;
		this.preUpdateCallbacks = preUpdateCallbacks;
		this.postUpdateCallbacks = postUpdateCallbacks;
		this.preRemoveCallbacks = preRemoveCallbacks;
		this.postRemoveCallbacks = postRemoveCallbacks;
		this.postLoadCallbacks = postLoadCallbacks;
	}

	private List<Callback<E>> getCallbacks(CallbackType callbackType) {
		return switch ( callbackType ) {
			case PRE_PERSIST -> preCreateCallbacks;
			case POST_PERSIST -> postCreateCallbacks;
			case PRE_UPDATE -> preUpdateCallbacks;
			case POST_UPDATE -> postUpdateCallbacks;
			case PRE_REMOVE -> preRemoveCallbacks;
			case POST_REMOVE -> postRemoveCallbacks;
			case POST_LOAD -> postLoadCallbacks;
		};
	}

	@Override
	public boolean hasRegisteredCallbacks(CallbackType callbackType) {
		return CollectionHelper.isNotEmpty( getCallbacks( callbackType ) );
	}

	@Override
	public boolean preCreate(E entity) {
		return callback( preCreateCallbacks, entity );
	}

	@Override
	public boolean postCreate(E entity) {
		return callback( postCreateCallbacks, entity );
	}

	@Override
	public boolean preUpdate(E entity) {
		return callback( preUpdateCallbacks, entity );
	}

	@Override
	public boolean postUpdate(E entity) {
		return callback( postUpdateCallbacks, entity );
	}

	@Override
	public boolean preRemove(E entity) {
		return callback( preRemoveCallbacks, entity );
	}

	@Override
	public boolean postRemove(E entity) {
		return callback( postRemoveCallbacks, entity );
	}

	@Override
	public boolean postLoad(E entity) {
		return callback( postLoadCallbacks, entity );
	}

	private boolean callback(List<Callback<E>> callbacks, E entity) {
		if ( CollectionHelper.isNotEmpty( callbacks ) ) {
			for ( Callback<E> callback : callbacks ) {
				callback.performCallback( entity );
			}
			return true;
		}
		else {
			return false;
		}
	}

	/// @see jakarta.persistence.EntityManagerFactory#addListener
	public EntityListenerRegistration addListener(CallbackType type, Consumer<E> listener) {
		final List<Callback<E>> callbacks = getCallbacks( type );
		var callback = new AddedCallbackImpl<>( type, listener );
		callbacks.add( callback );
		return new EntityListenerRegistrationImpl<>( this, type, callback );
	}

	/// @see jakarta.persistence.EntityManagerFactory#addListener
	public EntityListenerRegistration addListener(CallbackType type, Callback<E> callback) {
		final List<Callback<E>> callbacks = getCallbacks( type );
		callbacks.add( callback );
		return new EntityListenerRegistrationImpl<>( this, type, callback );
	}

	/// package-protected access to remove a callback used to support
	/// JPA's [jakarta.persistence.EntityListenerRegistration].
	void remove(CallbackType type, Callback<E> callback) {
		final List<Callback<E>> callbacks = getCallbacks( type );
		callbacks.remove( callback );
	}

	@Override
	public void release() {
		// todo (jpa4) : needed?
	}

	public static class Builder<E> {
		private final List<Callback<E>> preCreateCallbacks = new ArrayList<>();
		private final List<Callback<E>> postCreateCallbacks = new ArrayList<>();

		private final List<Callback<E>> preUpdateCallbacks = new ArrayList<>();
		private final List<Callback<E>> postUpdateCallbacks = new ArrayList<>();

		private final List<Callback<E>> preRemoveCallbacks = new ArrayList<>();
		private final List<Callback<E>> postRemoveCallbacks = new ArrayList<>();

		private final List<Callback<E>> postLoadCallbacks = new ArrayList<>();

		public void registerCallback(Callback<E> callback) {
			getCallbacks( callback.getCallbackType() ).add( callback );
		}

		private List<Callback<E>> getCallbacks(CallbackType callbackType) {
			return switch ( callbackType ) {
				case PRE_PERSIST -> preCreateCallbacks;
				case POST_PERSIST -> postCreateCallbacks;
				case PRE_UPDATE -> preUpdateCallbacks;
				case POST_UPDATE -> postUpdateCallbacks;
				case PRE_REMOVE -> preRemoveCallbacks;
				case POST_REMOVE -> postRemoveCallbacks;
				case POST_LOAD -> postLoadCallbacks;
			};
		}

		public EntityCallbacksImpl<E> build() {
			return new EntityCallbacksImpl<>(
					preCreateCallbacks,
					postCreateCallbacks,
					preUpdateCallbacks,
					postUpdateCallbacks,
					preRemoveCallbacks,
					postRemoveCallbacks,
					postLoadCallbacks
			);
		}
	}
}

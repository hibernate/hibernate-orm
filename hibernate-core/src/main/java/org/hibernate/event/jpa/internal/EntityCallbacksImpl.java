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
	private final List<Callback<? super E>> preCreateCallbacks;
	private final List<Callback<? super E>> postCreateCallbacks;

	private final List<Callback<? super E>> preUpdateCallbacks;
	private final List<Callback<? super E>> postUpdateCallbacks;

	private final List<Callback<? super E>> preRemoveCallbacks;
	private final List<Callback<? super E>> postRemoveCallbacks;

	private final List<Callback<? super E>> postLoadCallbacks;

	private EntityCallbacksImpl(
			List<Callback<? super E>> preCreateCallbacks,
			List<Callback<? super E>> postCreateCallbacks,
			List<Callback<? super E>> preUpdateCallbacks,
			List<Callback<? super E>> postUpdateCallbacks,
			List<Callback<? super E>> preRemoveCallbacks,
			List<Callback<? super E>> postRemoveCallbacks,
			List<Callback<? super E>> postLoadCallbacks) {
		this.preCreateCallbacks = preCreateCallbacks;
		this.postCreateCallbacks = postCreateCallbacks;
		this.preUpdateCallbacks = preUpdateCallbacks;
		this.postUpdateCallbacks = postUpdateCallbacks;
		this.preRemoveCallbacks = preRemoveCallbacks;
		this.postRemoveCallbacks = postRemoveCallbacks;
		this.postLoadCallbacks = postLoadCallbacks;
	}

	private List<Callback<? super E>> getCallbacks(CallbackType callbackType) {
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
	public <S extends E> boolean preCreate(S entity) {
		return callback( preCreateCallbacks, entity );
	}

	@Override
	public <S extends E> boolean postCreate(S entity) {
		return callback( postCreateCallbacks, entity );
	}

	@Override
	public <S extends E> boolean preUpdate(S entity) {
		return callback( preUpdateCallbacks, entity );
	}

	@Override
	public <S extends E> boolean postUpdate(S entity) {
		return callback( postUpdateCallbacks, entity );
	}

	@Override
	public <S extends E> boolean preRemove(S entity) {
		return callback( preRemoveCallbacks, entity );
	}

	@Override
	public <S extends E> boolean postRemove(S entity) {
		return callback( postRemoveCallbacks, entity );
	}

	@Override
	public <S extends E> boolean postLoad(S entity) {
		return callback( postLoadCallbacks, entity );
	}

	private boolean callback(List<Callback<? super E>> callbacks, E entity) {
		if ( CollectionHelper.isNotEmpty( callbacks ) ) {
			for ( Callback<? super E> callback : callbacks ) {
				callback.performCallback( entity );
			}
			return true;
		}
		else {
			return false;
		}
	}

	/// @see jakarta.persistence.EntityManagerFactory#addListener
	public EntityListenerRegistration addListener(CallbackType type, Consumer<? super E> listener) {
		final List<Callback<? super E>> callbacks = getCallbacks( type );
		var callback = new AddedCallbackImpl<>( type, listener );
		callbacks.add( callback );
		return new EntityListenerRegistrationImpl<>( this, type, callback );
	}

	/// @see jakarta.persistence.EntityManagerFactory#addListener
	public EntityListenerRegistration addListener(CallbackType type, Callback<? super E> callback) {
		final List<Callback<? super E>> callbacks = getCallbacks( type );
		callbacks.add( callback );
		return new EntityListenerRegistrationImpl<>( this, type, callback );
	}

	/// package-protected access to remove a callback used to support
	/// JPA's [jakarta.persistence.EntityListenerRegistration].
	void remove(CallbackType type, Callback<? super E> callback) {
		final List<Callback<? super E>> callbacks = getCallbacks( type );
		callbacks.remove( callback );
	}

	public static class Builder<E> {
		private final List<Callback<? super E>> preCreateCallbacks = new ArrayList<>();
		private final List<Callback<? super E>> postCreateCallbacks = new ArrayList<>();

		private final List<Callback<? super E>> preUpdateCallbacks = new ArrayList<>();
		private final List<Callback<? super E>> postUpdateCallbacks = new ArrayList<>();

		private final List<Callback<? super E>> preRemoveCallbacks = new ArrayList<>();
		private final List<Callback<? super E>> postRemoveCallbacks = new ArrayList<>();

		private final List<Callback<? super E>> postLoadCallbacks = new ArrayList<>();

		public void registerCallback(Callback<? super E> callback) {
			getCallbacks( callback.getCallbackType() ).add( callback );
		}

		private List<Callback<? super E>> getCallbacks(CallbackType callbackType) {
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

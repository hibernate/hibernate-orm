/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jpa.internal;

import jakarta.persistence.EntityListenerRegistration;
import org.hibernate.event.jpa.spi.EntityCallbacks;
import org.hibernate.event.jpa.spi.Callback;
import org.hibernate.jpa.event.spi.CallbackType;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import jakarta.annotation.Nonnull;

/// Implementation of [EntityCallbacks]
///
/// @author Steve Ebersole
public class EntityCallbacksImpl<E> implements EntityCallbacks<E>, Serializable {
	private final Map<CallbackType, List<Callback<? super E>>> callbacks;

	private EntityCallbacksImpl(@Nonnull Map<CallbackType, List<Callback<? super E>>> callbacks) {
		this.callbacks = callbacks;
	}

	@Nonnull
	private List<Callback<? super E>> getCallbacks(@Nonnull CallbackType callbackType) {
		return callbacks.get( callbackType );
	}

	@Override
	public boolean hasRegisteredCallbacks(@Nonnull CallbackType callbackType) {
		return isNotEmpty( getCallbacks( callbackType ) );
	}

	@Override
	public <S extends E> boolean preCreate(@Nonnull S entity) {
		return callback( CallbackType.PRE_PERSIST, entity );
	}

	@Override
	public <S extends E> boolean postCreate(@Nonnull S entity) {
		return callback( CallbackType.POST_PERSIST, entity );
	}

	@Override
	public <S extends E> boolean preMerge(@Nonnull S entity) {
		return callback( CallbackType.PRE_MERGE, entity );
	}

	@Override
	public <S extends E> boolean preInsert(@Nonnull S entity) {
		return callback( CallbackType.PRE_INSERT, entity );
	}

	@Override
	public <S extends E> boolean postInsert(@Nonnull S entity) {
		return callback( CallbackType.POST_INSERT, entity );
	}

	@Override
	public <S extends E> boolean preUpdate(@Nonnull S entity) {
		return callback( CallbackType.PRE_UPDATE, entity );
	}

	@Override
	public <S extends E> boolean postUpdate(@Nonnull S entity) {
		return callback( CallbackType.POST_UPDATE, entity );
	}

	@Override
	public <S extends E> boolean preUpsert(@Nonnull S entity) {
		return callback( CallbackType.PRE_UPSERT, entity );
	}

	@Override
	public <S extends E> boolean postUpsert(@Nonnull S entity) {
		return callback( CallbackType.POST_UPSERT, entity );
	}

	@Override
	public <S extends E> boolean preRemove(@Nonnull S entity) {
		return callback( CallbackType.PRE_REMOVE, entity );
	}

	@Override
	public <S extends E> boolean postRemove(@Nonnull S entity) {
		return callback( CallbackType.POST_REMOVE, entity );
	}

	@Override
	public <S extends E> boolean preDelete(@Nonnull S entity) {
		return callback( CallbackType.PRE_DELETE, entity );
	}

	@Override
	public <S extends E> boolean postDelete(@Nonnull S entity) {
		return callback( CallbackType.POST_DELETE, entity );
	}

	@Override
	public <S extends E> boolean postLoad(@Nonnull S entity) {
		return callback( CallbackType.POST_LOAD, entity );
	}

	private boolean callback(@Nonnull CallbackType callbackType, @Nonnull E entity) {
		return callback( getCallbacks( callbackType ), entity );
	}

	private boolean callback(@Nonnull List<Callback<? super E>> callbacks, @Nonnull E entity) {
		if ( isNotEmpty( callbacks ) ) {
			for ( var callback : callbacks ) {
				callback.performCallback( entity );
			}
			return true;
		}
		else {
			return false;
		}
	}

	/// @see jakarta.persistence.EntityManagerFactory#addListener
	@Nonnull
	public EntityListenerRegistration addListener(@Nonnull CallbackType type, @Nonnull Consumer<? super E> listener) {
		final var callbacks = getCallbacks( type );
		var callback = new AddedCallbackImpl<>( type, listener );
		callbacks.add( callback );
		return new EntityListenerRegistrationImpl<>( this, type, callback );
	}

	/// @see jakarta.persistence.EntityManagerFactory#addListener
	@Nonnull
	public EntityListenerRegistration addListener(@Nonnull CallbackType type, @Nonnull Callback<? super E> callback) {
		final var callbacks = getCallbacks( type );
		callbacks.add( callback );
		return new EntityListenerRegistrationImpl<>( this, type, callback );
	}

	/// package-protected access to remove a callback used to support
	/// JPA's [jakarta.persistence.EntityListenerRegistration].
	void remove(@Nonnull CallbackType type, @Nonnull Callback<? super E> callback) {
		final var callbacks = getCallbacks( type );
		callbacks.remove( callback );
	}

	public static class Builder<E> {
		private final EnumMap<CallbackType, List<Callback<? super E>>> callbacks = new EnumMap<>( CallbackType.class );

		public Builder() {
			for ( var type : CallbackType.values() ) {
				callbacks.put( type, new CopyOnWriteArrayList<>() );
			}
		}

		public void registerCallback(@Nonnull Callback<? super E> callback) {
			getCallbacks( callback.getCallbackType() ).add( callback );
		}

		@Nonnull
		private List<Callback<? super E>> getCallbacks(@Nonnull CallbackType callbackType) {
			return callbacks.get( callbackType );
		}

		@Nonnull
		public EntityCallbacksImpl<E> build() {
			return new EntityCallbacksImpl<>( new EnumMap<>( callbacks ) );
		}
	}
}

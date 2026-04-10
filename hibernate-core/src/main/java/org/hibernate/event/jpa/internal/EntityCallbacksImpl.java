/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jpa.internal;

import jakarta.persistence.EntityListenerRegistration;
import jakarta.persistence.EntityManagerFactory;
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
	private final List<Callback<? super E>> preMergeCallbacks;

	private final List<Callback<? super E>> prePersistCallbacks;
	private final List<Callback<? super E>> postPersistCallbacks;

	private final List<Callback<? super E>> preInsertCallbacks;
	private final List<Callback<? super E>> postInsertCallbacks;

	private final List<Callback<? super E>> preUpdateCallbacks;
	private final List<Callback<? super E>> postUpdateCallbacks;

	private final List<Callback<? super E>> preUpsertCallbacks;
	private final List<Callback<? super E>> postUpsertCallbacks;

	private final List<Callback<? super E>> preRemoveCallbacks;
	private final List<Callback<? super E>> postRemoveCallbacks;

	private final List<Callback<? super E>> preDeleteCallbacks;
	private final List<Callback<? super E>> postDeleteCallbacks;

	private final List<Callback<? super E>> postLoadCallbacks;

	private EntityCallbacksImpl(
			List<Callback<? super E>> preMergeCallbacks,
			List<Callback<? super E>> prePersistCallbacks,
			List<Callback<? super E>> postPersistCallbacks,
			List<Callback<? super E>> preInsertCallbacks,
			List<Callback<? super E>> postInsertCallbacks,
			List<Callback<? super E>> preUpdateCallbacks,
			List<Callback<? super E>> postUpdateCallbacks,
			List<Callback<? super E>> preUpsertCallbacks,
			List<Callback<? super E>> postUpsertCallbacks,
			List<Callback<? super E>> preRemoveCallbacks,
			List<Callback<? super E>> postRemoveCallbacks,
			List<Callback<? super E>> preDeleteCallbacks,
			List<Callback<? super E>> postDeleteCallbacks,
			List<Callback<? super E>> postLoadCallbacks) {
		this.preMergeCallbacks = preMergeCallbacks;
		this.prePersistCallbacks = prePersistCallbacks;
		this.postPersistCallbacks = postPersistCallbacks;
		this.preInsertCallbacks = preInsertCallbacks;
		this.postInsertCallbacks = postInsertCallbacks;
		this.preUpdateCallbacks = preUpdateCallbacks;
		this.postUpdateCallbacks = postUpdateCallbacks;
		this.preUpsertCallbacks = preUpsertCallbacks;
		this.postUpsertCallbacks = postUpsertCallbacks;
		this.preRemoveCallbacks = preRemoveCallbacks;
		this.postRemoveCallbacks = postRemoveCallbacks;
		this.preDeleteCallbacks = preDeleteCallbacks;
		this.postDeleteCallbacks = postDeleteCallbacks;
		this.postLoadCallbacks = postLoadCallbacks;
	}

	private List<Callback<? super E>> getCallbacks(CallbackType callbackType) {
		return switch ( callbackType ) {
			case PRE_MERGE -> preMergeCallbacks;
			case PRE_PERSIST -> prePersistCallbacks;
			case POST_PERSIST -> postPersistCallbacks;
			case PRE_UPDATE -> preUpdateCallbacks;
			case POST_UPDATE -> postUpdateCallbacks;
			case PRE_UPSERT -> preUpsertCallbacks;
			case POST_UPSERT -> postUpsertCallbacks;
			case PRE_INSERT -> preInsertCallbacks;
			case POST_INSERT -> postInsertCallbacks;
			case PRE_REMOVE -> preRemoveCallbacks;
			case POST_REMOVE -> postRemoveCallbacks;
			case PRE_DELETE -> preDeleteCallbacks;
			case POST_DELETE -> postDeleteCallbacks;
			case POST_LOAD -> postLoadCallbacks;
		};
	}

	@Override
	public boolean hasRegisteredCallbacks(CallbackType callbackType) {
		return CollectionHelper.isNotEmpty( getCallbacks( callbackType ) );
	}

	@Override
	public <S extends E> boolean preMerge(S entity) {
		return callback( preMergeCallbacks, entity );
	}

	@Override
	public <S extends E> boolean prePersist(S entity) {
		return callback( prePersistCallbacks, entity );
	}

	@Override
	public <S extends E> boolean postPersist(S entity) {
		return callback( postPersistCallbacks, entity );
	}

	@Override
	public <S extends E> boolean preInsert(S entity) {
		return callback( preInsertCallbacks, entity );
	}

	@Override
	public <S extends E> boolean postInsert(S entity) {
		return callback( postInsertCallbacks, entity );
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
	public <S extends E> boolean preUpsert(S entity) {
		return callback( preUpsertCallbacks, entity );
	}

	@Override
	public <S extends E> boolean postUpsert(S entity) {
		return callback( postUpsertCallbacks, entity );
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
	public <S extends E> boolean preDelete(S entity) {
		return callback( preDeleteCallbacks, entity );
	}

	@Override
	public <S extends E> boolean postDelete(S entity) {
		return callback( postDeleteCallbacks, entity );
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

	/// @see EntityManagerFactory#addListener
	public EntityListenerRegistration addListener(CallbackType type, Consumer<? super E> listener) {
		final List<Callback<? super E>> callbacks = getCallbacks( type );
		var callback = new AddedCallbackImpl<>( type, listener );
		callbacks.add( callback );
		return new EntityListenerRegistrationImpl<>( this, type, callback );
	}

	/// @see EntityManagerFactory#addListener
	public EntityListenerRegistration addListener(CallbackType type, Callback<? super E> callback) {
		final List<Callback<? super E>> callbacks = getCallbacks( type );
		callbacks.add( callback );
		return new EntityListenerRegistrationImpl<>( this, type, callback );
	}

	/// package-protected access to remove a callback used to support
	/// JPA's [EntityListenerRegistration].
	void remove(CallbackType type, Callback<? super E> callback) {
		final List<Callback<? super E>> callbacks = getCallbacks( type );
		callbacks.remove( callback );
	}

	public static class Builder<E> {
		private final List<Callback<? super E>> preMergeCallbacks = new ArrayList<>();

		private final List<Callback<? super E>> prePersistCallbacks = new ArrayList<>();
		private final List<Callback<? super E>> postPersistCallbacks = new ArrayList<>();

		private final List<Callback<? super E>> preInsertCallbacks = new ArrayList<>();
		private final List<Callback<? super E>> postInsertCallbacks = new ArrayList<>();

		private final List<Callback<? super E>> preUpdateCallbacks = new ArrayList<>();
		private final List<Callback<? super E>> postUpdateCallbacks = new ArrayList<>();

		private final List<Callback<? super E>> preUpsertCallbacks = new ArrayList<>();
		private final List<Callback<? super E>> postUpsertCallbacks = new ArrayList<>();

		private final List<Callback<? super E>> preRemoveCallbacks = new ArrayList<>();
		private final List<Callback<? super E>> postRemoveCallbacks = new ArrayList<>();

		private final List<Callback<? super E>> preDeleteCallbacks = new ArrayList<>();
		private final List<Callback<? super E>> postDeleteCallbacks = new ArrayList<>();

		private final List<Callback<? super E>> postLoadCallbacks = new ArrayList<>();

		public void registerCallback(Callback<? super E> callback) {
			getCallbacks( callback.getCallbackType() ).add( callback );
		}

		private List<Callback<? super E>> getCallbacks(CallbackType callbackType) {
			return switch ( callbackType ) {
				case PRE_MERGE -> preMergeCallbacks;
				case PRE_PERSIST -> prePersistCallbacks;
				case POST_PERSIST -> postPersistCallbacks;
				case PRE_UPDATE -> preUpdateCallbacks;
				case POST_UPDATE -> postUpdateCallbacks;
				case PRE_UPSERT -> preUpsertCallbacks;
				case POST_UPSERT -> postUpsertCallbacks;
				case PRE_INSERT -> preInsertCallbacks;
				case POST_INSERT -> postInsertCallbacks;
				case PRE_REMOVE -> preRemoveCallbacks;
				case POST_REMOVE -> postRemoveCallbacks;
				case PRE_DELETE -> preDeleteCallbacks;
				case POST_DELETE -> postDeleteCallbacks;
				case POST_LOAD -> postLoadCallbacks;
			};
		}

		public EntityCallbacksImpl<E> build() {
			return new EntityCallbacksImpl<>(
					preMergeCallbacks,
					prePersistCallbacks,
					postPersistCallbacks,
					preUpdateCallbacks,
					postUpdateCallbacks,
					preUpsertCallbacks,
					postUpsertCallbacks,
					preInsertCallbacks,
					postInsertCallbacks,
					preRemoveCallbacks,
					postRemoveCallbacks,
					preDeleteCallbacks,
					postDeleteCallbacks,
					postLoadCallbacks
			);
		}
	}
}

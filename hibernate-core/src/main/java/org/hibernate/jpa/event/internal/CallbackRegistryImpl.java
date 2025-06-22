/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.internal;

import java.util.HashMap;
import java.util.Map;


import org.hibernate.internal.build.AllowReflection;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.MapBackedClassValue;
import org.hibernate.internal.util.collections.ReadOnlyMap;
import org.hibernate.jpa.event.spi.Callback;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.jpa.event.spi.CallbackType;

/**
 * Keep track of all lifecycle callbacks and listeners for a given persistence unit
 *
 * @author Kabir Khan
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
final class CallbackRegistryImpl implements CallbackRegistry {

	private final ReadOnlyMap<Class<?>,Callback[]> preCreates;
	private final ReadOnlyMap<Class<?>,Callback[]> postCreates;
	private final ReadOnlyMap<Class<?>,Callback[]> preRemoves;
	private final ReadOnlyMap<Class<?>,Callback[]> postRemoves;
	private final ReadOnlyMap<Class<?>,Callback[]> preUpdates;
	private final ReadOnlyMap<Class<?>,Callback[]> postUpdates;
	private final ReadOnlyMap<Class<?>,Callback[]> postLoads;

	public CallbackRegistryImpl(
			Map<Class<?>, Callback[]> preCreates,
			Map<Class<?>, Callback[]> postCreates,
			Map<Class<?>, Callback[]> preRemoves,
			Map<Class<?>, Callback[]> postRemoves,
			Map<Class<?>, Callback[]> preUpdates,
			Map<Class<?>, Callback[]> postUpdates,
			Map<Class<?>, Callback[]> postLoads) {
		this.preCreates = createBackingMap( preCreates );
		this.postCreates = createBackingMap( postCreates );
		this.preRemoves = createBackingMap( preRemoves );
		this.postRemoves = createBackingMap( postRemoves );
		this.preUpdates = createBackingMap( preUpdates );
		this.postUpdates = createBackingMap( postUpdates );
		this.postLoads = createBackingMap( postLoads );
	}

	private static ReadOnlyMap<Class<?>, Callback[]> createBackingMap(final Map<Class<?>, Callback[]> src) {
		return src == null || src.isEmpty()
				? ReadOnlyMap.EMPTY
				: new MapBackedClassValue<>( src );
	}

	@Override
	public boolean hasRegisteredCallbacks(Class<?> entityClass, CallbackType callbackType) {
		return notEmpty( getCallbackMap( callbackType ).get( entityClass ) );
	}

	@Override
	public void preCreate(Object bean) {
		callback( preCreates.get( bean.getClass() ), bean );
	}

	private boolean notEmpty(Callback[] callbacks) {
		return callbacks != null && callbacks.length > 0;
	}

	@Override
	public void postCreate(Object bean) {
		callback( postCreates.get( bean.getClass() ), bean );
	}

	@Override
	public boolean preUpdate(Object bean) {
		return callback( preUpdates.get( bean.getClass() ), bean );
	}

	@Override
	public void postUpdate(Object bean) {
		callback( postUpdates.get( bean.getClass() ), bean );
	}

	@Override
	public void preRemove(Object bean) {
		callback( preRemoves.get( bean.getClass() ), bean );
	}

	@Override
	public void postRemove(Object bean) {
		callback( postRemoves.get( bean.getClass() ), bean );
	}

	@Override
	public boolean postLoad(Object bean) {
		return callback( postLoads.get( bean.getClass() ), bean );
	}

	@Override
	public void release() {
		this.preCreates.dispose();
		this.postCreates.dispose();
		this.preRemoves.dispose();
		this.postRemoves.dispose();
		this.preUpdates.dispose();
		this.postUpdates.dispose();
		this.postLoads.dispose();
	}

	private boolean callback(Callback[] callbacks, Object bean) {
		if ( callbacks != null && callbacks.length != 0 ) {
			for ( Callback callback : callbacks ) {
				callback.performCallback( bean );
			}
			return true;
		}
		else {
			return false;
		}
	}

	private ReadOnlyMap<Class<?>,Callback[]> getCallbackMap(CallbackType callbackType) {
		return switch ( callbackType ) {
			case PRE_PERSIST -> preCreates;
			case POST_PERSIST -> postCreates;
			case PRE_REMOVE -> preRemoves;
			case POST_REMOVE -> postRemoves;
			case PRE_UPDATE -> preUpdates;
			case POST_UPDATE -> postUpdates;
			case POST_LOAD -> postLoads;
		};
	}

	public static class Builder {
		private static final Callback[] NO_CALLBACKS = new Callback[0];

		private final Map<Class<?>, Callback[]> preCreates = new HashMap<>();
		private final Map<Class<?>, Callback[]> postCreates = new HashMap<>();
		private final Map<Class<?>, Callback[]> preRemoves = new HashMap<>();
		private final Map<Class<?>, Callback[]> postRemoves = new HashMap<>();
		private final Map<Class<?>, Callback[]> preUpdates = new HashMap<>();
		private final Map<Class<?>, Callback[]> postUpdates = new HashMap<>();
		private final Map<Class<?>, Callback[]> postLoads = new HashMap<>();

		@AllowReflection
		public void registerCallbacks(Class<?> entityClass, Callback[] callbacks) {
			if ( callbacks != null ) {
				for ( Callback callback : callbacks ) {
					addCallback( entityClass, callback );
				}
			}
		}

		public void addCallback(Class<?> entityClass, Callback callback) {
			final var callbackMap = getCallbackMap( callback.getCallbackType() );
			final Callback[] existingCallbacks = callbackMap.getOrDefault( entityClass, NO_CALLBACKS );
			callbackMap.put( entityClass, ArrayHelper.add( existingCallbacks, callback ) );
		}

		private Map<Class<?>, Callback[]> getCallbackMap(CallbackType callbackType) {
			return switch ( callbackType ) {
				case PRE_PERSIST -> preCreates;
				case POST_PERSIST -> postCreates;
				case PRE_REMOVE -> preRemoves;
				case POST_REMOVE -> postRemoves;
				case PRE_UPDATE -> preUpdates;
				case POST_UPDATE -> postUpdates;
				case POST_LOAD -> postLoads;
			};
		}

		protected CallbackRegistry build() {
			return new CallbackRegistryImpl( preCreates, postCreates, preRemoves, postRemoves, preUpdates, postUpdates, postLoads );
		}

	}
}

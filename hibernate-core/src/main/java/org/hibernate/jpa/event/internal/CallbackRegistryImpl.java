/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.internal;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.PersistenceException;

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

	private final ReadOnlyMap<Class,Callback[]> preCreates;
	private final ReadOnlyMap<Class,Callback[]> postCreates;
	private final ReadOnlyMap<Class,Callback[]> preRemoves;
	private final ReadOnlyMap<Class,Callback[]> postRemoves;
	private final ReadOnlyMap<Class,Callback[]> preUpdates;
	private final ReadOnlyMap<Class,Callback[]> postUpdates;
	private final ReadOnlyMap<Class,Callback[]> postLoads;

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

	private static ReadOnlyMap<Class, Callback[]> createBackingMap(final Map<Class<?>, Callback[]> src) {
		if ( src == null || src.isEmpty() ) {
			return ReadOnlyMap.EMPTY;
		}
		else {
			return new MapBackedClassValue<>( src );
		}
	}

	@Override
	public boolean hasRegisteredCallbacks(Class<?> entityClass, CallbackType callbackType) {
		final ReadOnlyMap<Class,Callback[]> map = determineAppropriateCallbackMap( callbackType );
		return notEmpty( map.get( entityClass ) );
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

	private ReadOnlyMap<Class,Callback[]> determineAppropriateCallbackMap(CallbackType callbackType) {
		if ( callbackType == CallbackType.PRE_PERSIST ) {
			return preCreates;
		}

		if ( callbackType == CallbackType.POST_PERSIST ) {
			return postCreates;
		}

		if ( callbackType == CallbackType.PRE_REMOVE ) {
			return preRemoves;
		}

		if ( callbackType == CallbackType.POST_REMOVE ) {
			return postRemoves;
		}

		if ( callbackType == CallbackType.PRE_UPDATE ) {
			return preUpdates;
		}

		if ( callbackType == CallbackType.POST_UPDATE ) {
			return postUpdates;
		}

		if ( callbackType == CallbackType.POST_LOAD ) {
			return postLoads;
		}

		throw new PersistenceException( "Unrecognized JPA callback type [" + callbackType + "]" );
	}

	public static class Builder {
		private final Map<Class<?>, Callback[]> preCreates = new HashMap<>();
		private final Map<Class<?>, Callback[]> postCreates = new HashMap<>();
		private final Map<Class<?>, Callback[]> preRemoves = new HashMap<>();
		private final Map<Class<?>, Callback[]> postRemoves = new HashMap<>();
		private final Map<Class<?>, Callback[]> preUpdates = new HashMap<>();
		private final Map<Class<?>, Callback[]> postUpdates = new HashMap<>();
		private final Map<Class<?>, Callback[]> postLoads = new HashMap<>();

		public void registerCallbacks(Class<?> entityClass, Callback[] callbacks) {
			if ( callbacks == null || callbacks.length == 0 ) {
				return;
			}

			for ( Callback callback : callbacks ) {
				final Map<Class<?>, Callback[]> map = determineAppropriateCallbackMap( callback.getCallbackType() );
				Callback[] entityCallbacks = map.get( entityClass );
				if ( entityCallbacks == null ) {
					entityCallbacks = new Callback[0];
				}
				entityCallbacks = ArrayHelper.join( entityCallbacks, callback );
				map.put( entityClass, entityCallbacks );
			}
		}

		private Map<Class<?>, Callback[]> determineAppropriateCallbackMap(CallbackType callbackType) {
			if ( callbackType == CallbackType.PRE_PERSIST ) {
				return preCreates;
			}

			if ( callbackType == CallbackType.POST_PERSIST ) {
				return postCreates;
			}

			if ( callbackType == CallbackType.PRE_REMOVE ) {
				return preRemoves;
			}

			if ( callbackType == CallbackType.POST_REMOVE ) {
				return postRemoves;
			}

			if ( callbackType == CallbackType.PRE_UPDATE ) {
				return preUpdates;
			}

			if ( callbackType == CallbackType.POST_UPDATE ) {
				return postUpdates;
			}

			if ( callbackType == CallbackType.POST_LOAD ) {
				return postLoads;
			}

			throw new PersistenceException( "Unrecognized JPA callback type [" + callbackType + "]" );
		}

		protected CallbackRegistryImpl build() {
			return new CallbackRegistryImpl( preCreates, postCreates, preRemoves, postRemoves, preUpdates, postUpdates, postLoads );
		}

	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.internal.jpa;

import java.util.HashMap;
import javax.persistence.PersistenceException;

import org.hibernate.jpa.event.spi.jpa.Callback;
import org.hibernate.jpa.event.spi.jpa.CallbackRegistry;
import org.hibernate.jpa.event.spi.jpa.CallbackType;
import org.hibernate.jpa.event.spi.jpa.CallbackBuilder;

/**
 * Keep track of all lifecycle callbacks and listeners for a given persistence unit
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @author Steve Ebersole
 */
@SuppressWarnings({"unchecked", "serial"})
public class CallbackRegistryImpl implements CallbackRegistry, CallbackBuilder.CallbackRegistrar {
	private HashMap<Class, Callback[]> preCreates = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> postCreates = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> preRemoves = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> postRemoves = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> preUpdates = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> postUpdates = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> postLoads = new HashMap<Class, Callback[]>();

	@Override
	public boolean hasRegisteredCallbacks(Class entityClass, CallbackType callbackType) {
		final HashMap<Class, Callback[]> map = determineAppropriateCallbackMap( callbackType );
		return notEmpty( map.get( entityClass ) );
	}

	@Override
	public void registerCallbacks(Class entityClass, Callback[] callbacks) {
		if ( callbacks == null || callbacks.length == 0 ) {
			return;
		}

		final HashMap<Class, Callback[]> map = determineAppropriateCallbackMap( callbacks[0].getCallbackType() );
		if ( map.containsKey( entityClass ) ) {
			throw new PersistenceException( "Error build callback listeners; entity [" + entityClass.getName() + " was already processed" );
		}
		map.put( entityClass, callbacks );
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

	private HashMap<Class, Callback[]> determineAppropriateCallbackMap(CallbackType callbackType) {
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

	public void release() {
		preCreates.clear();
		postCreates.clear();

		preRemoves.clear();
		postRemoves.clear();

		preUpdates.clear();
		postUpdates.clear();

		postLoads.clear();
	}


	// deprecations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean hasPostCreateCallbacks(Class entityClass) {
		return notEmpty( preCreates.get( entityClass ) );
	}

	@Override
	public boolean hasPostUpdateCallbacks(Class entityClass) {
		return notEmpty( postUpdates.get( entityClass ) );
	}

	@Override
	public boolean hasPostRemoveCallbacks(Class entityClass) {
		return notEmpty( postRemoves.get( entityClass ) );
	}

	@Override
	public boolean hasRegisteredCallbacks(Class entityClass, Class annotationClass) {
		final HashMap<Class, Callback[]> map = determineAppropriateCallbackMap( toCallbackType( annotationClass ) );
		return map != null && map.containsKey( entityClass );
	}

	private CallbackType toCallbackType(Class annotationClass) {
		if ( annotationClass == CallbackType.POST_LOAD.getCallbackAnnotation() ) {
			return CallbackType.POST_LOAD;
		}
		else if ( annotationClass == CallbackType.PRE_PERSIST.getCallbackAnnotation() ) {
			return CallbackType.PRE_PERSIST;
		}
		else if ( annotationClass == CallbackType.POST_PERSIST.getCallbackAnnotation() ) {
			return CallbackType.POST_PERSIST;
		}
		else if ( annotationClass == CallbackType.PRE_UPDATE.getCallbackAnnotation() ) {
			return CallbackType.PRE_UPDATE;
		}
		else if ( annotationClass == CallbackType.POST_UPDATE.getCallbackAnnotation() ) {
			return CallbackType.POST_UPDATE;
		}
		else if ( annotationClass == CallbackType.PRE_REMOVE.getCallbackAnnotation() ) {
			return CallbackType.PRE_REMOVE;
		}
		else if ( annotationClass == CallbackType.POST_REMOVE.getCallbackAnnotation() ) {
			return CallbackType.POST_REMOVE;
		}

		throw new PersistenceException( "Unrecognized JPA callback annotation [" + annotationClass + "]" );
	}
}

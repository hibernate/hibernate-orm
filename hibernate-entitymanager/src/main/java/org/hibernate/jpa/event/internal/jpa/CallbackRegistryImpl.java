/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.event.internal.jpa;

import java.util.HashMap;
import javax.persistence.PersistenceException;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.hibernate.jpa.event.spi.jpa.Callback;
import org.hibernate.jpa.event.spi.jpa.CallbackRegistry;

/**
 * Keep track of all lifecycle callbacks and listeners for a given persistence unit
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @author Steve Ebersole
 */
@SuppressWarnings({"unchecked", "serial"})
public class CallbackRegistryImpl implements CallbackRegistry {
	private HashMap<Class, Callback[]> preCreates = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> postCreates = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> preRemoves = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> postRemoves = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> preUpdates = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> postUpdates = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> postLoads = new HashMap<Class, Callback[]>();

	@Override
	public void preCreate(Object bean) {
		callback( preCreates.get( bean.getClass() ), bean );
	}

	@Override
	public boolean hasPostCreateCallbacks(Class entityClass) {
		return notEmpty( preCreates.get( entityClass ) );
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
	public boolean hasPostUpdateCallbacks(Class entityClass) {
		return notEmpty( postUpdates.get( entityClass ) );
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
	public boolean hasPostRemoveCallbacks(Class entityClass) {
		return notEmpty( postRemoves.get( entityClass ) );
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


	/**
	 * Great care should be taken calling this.  Not a fan of it being public, but that is needed because of
	 * @param entityClass
	 * @param annotationClass
	 * @param callbacks
	 */
	public void addEntityCallbacks(Class entityClass, Class annotationClass, Callback[] callbacks) {
		final HashMap<Class, Callback[]> map = determineAppropriateCallbackMap( annotationClass );
		if ( map.containsKey( entityClass ) ) {
			throw new PersistenceException( "Error build callback listeners; entity [" + entityClass.getName() + " was already processed" );
		}
		map.put( entityClass, callbacks );
	}

	private HashMap<Class, Callback[]> determineAppropriateCallbackMap(Class annotationClass) {
		if ( PrePersist.class.equals( annotationClass ) ) {
			return preCreates;
		}

		if ( PostPersist.class.equals( annotationClass ) ) {
			return postCreates;
		}

		if ( PreRemove.class.equals( annotationClass ) ) {
			return preRemoves;
		}

		if ( PostRemove.class.equals( annotationClass ) ) {
			return postRemoves;
		}

		if ( PreUpdate.class.equals( annotationClass ) ) {
			return preUpdates;
		}

		if ( PostUpdate.class.equals( annotationClass ) ) {
			return postUpdates;
		}

		if ( PostLoad.class.equals( annotationClass ) ) {
			return postLoads;
		}

		throw new PersistenceException( "Unrecognized JPA callback annotation [" + annotationClass.getName() + "]" );
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
}

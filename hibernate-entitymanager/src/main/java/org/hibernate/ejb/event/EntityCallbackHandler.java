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
package org.hibernate.ejb.event;

import java.io.Serializable;
import java.util.HashMap;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * Keep track of all lifecycle callbacks and listeners for a given persistence unit
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
@SuppressWarnings({"unchecked", "serial"})
public class EntityCallbackHandler implements Serializable {
	private HashMap<Class, Callback[]> preCreates = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> postCreates = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> preRemoves = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> postRemoves = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> preUpdates = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> postUpdates = new HashMap<Class, Callback[]>();
	private HashMap<Class, Callback[]> postLoads = new HashMap<Class, Callback[]>();

	public void add(XClass entity, ReflectionManager reflectionManager) {
		addCallback( entity, preCreates, PrePersist.class, reflectionManager );
		addCallback( entity, postCreates, PostPersist.class, reflectionManager );
		addCallback( entity, preRemoves, PreRemove.class, reflectionManager );
		addCallback( entity, postRemoves, PostRemove.class, reflectionManager );
		addCallback( entity, preUpdates, PreUpdate.class, reflectionManager );
		addCallback( entity, postUpdates, PostUpdate.class, reflectionManager );
		addCallback( entity, postLoads, PostLoad.class, reflectionManager );
	}

	public void add( Class entity,
	                 ClassLoaderService classLoaderService,
	                 EntityBinding binding ) {
        addCallback( entity, preCreates, PrePersist.class, classLoaderService, binding );
        addCallback( entity, postCreates, PostPersist.class, classLoaderService, binding );
        addCallback( entity, preRemoves, PreRemove.class, classLoaderService, binding );
        addCallback( entity, postRemoves, PostRemove.class, classLoaderService, binding );
        addCallback( entity, preUpdates, PreUpdate.class, classLoaderService, binding );
        addCallback( entity, postUpdates, PostUpdate.class, classLoaderService, binding );
        addCallback( entity, postLoads, PostLoad.class, classLoaderService, binding );
	}

	public boolean preCreate(Object bean) {
		if(preCreates.isEmpty()) {
			return false;
		}
		return callback( preCreates.get( bean.getClass() ), bean );
	}

	public boolean postCreate(Object bean) {
		if(postCreates.isEmpty()) {
			return false;
		}
		return callback( postCreates.get( bean.getClass() ), bean );
	}

	public boolean preRemove(Object bean) {
		if(preRemoves.isEmpty()) {
			return false;
		}
		return callback( preRemoves.get( bean.getClass() ), bean );
	}

	public boolean postRemove(Object bean) {
		if(postRemoves.isEmpty()) {
			return false;
		}
		return callback( postRemoves.get( bean.getClass() ), bean );
	}

	public boolean preUpdate(Object bean) {
		if(preUpdates.isEmpty()) {
			return false;
		}
		return callback( preUpdates.get( bean.getClass() ), bean );
	}

	public boolean postUpdate(Object bean) {
		if(postUpdates.isEmpty()) {
			return false;
		}
		return callback( postUpdates.get( bean.getClass() ), bean );
	}

	public boolean postLoad(Object bean) {
		if(postLoads.isEmpty()) {
			return false;
		}
		return callback( postLoads.get( bean.getClass() ), bean );
	}


	private boolean callback(Callback[] callbacks, Object bean) {
		if ( callbacks != null && callbacks.length != 0 ) {
			for ( Callback callback : callbacks ) {
				callback.invoke( bean );
			}
			return true;
		}
		else {
			return false;
		}
	}

	private void addCallback(
			XClass entity, HashMap<Class, Callback[]> map, Class annotation, ReflectionManager reflectionManager
	) {
		Callback[] callbacks = null;
		callbacks = CallbackResolver.resolveCallback( entity, annotation, reflectionManager );
		map.put( reflectionManager.toClass( entity ), callbacks );
	}

    private void addCallback( Class<?> entity,
                              HashMap<Class, Callback[]> map,
                              Class annotation,
                              ClassLoaderService classLoaderService,
                              EntityBinding binding ) {
        map.put(entity, CallbackResolver.resolveCallbacks(entity, annotation, classLoaderService, binding));
    }
}

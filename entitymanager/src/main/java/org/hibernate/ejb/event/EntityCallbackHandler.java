/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.ReflectionManager;

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

	public boolean preCreate(Object bean) {
		return callback( preCreates.get( bean.getClass() ), bean );
	}

	public boolean postCreate(Object bean) {
		return callback( postCreates.get( bean.getClass() ), bean );
	}

	public boolean preRemove(Object bean) {
		return callback( preRemoves.get( bean.getClass() ), bean );
	}

	public boolean postRemove(Object bean) {
		return callback( postRemoves.get( bean.getClass() ), bean );
	}

	public boolean preUpdate(Object bean) {
		return callback( preUpdates.get( bean.getClass() ), bean );
	}

	public boolean postUpdate(Object bean) {
		return callback( postUpdates.get( bean.getClass() ), bean );
	}

	public boolean postLoad(Object bean) {
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
}

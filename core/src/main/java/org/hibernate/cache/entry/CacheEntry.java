/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
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
 *
 */
package org.hibernate.cache.entry;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.EventSource;
import org.hibernate.event.PreLoadEvent;
import org.hibernate.event.PreLoadEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.ArrayHelper;

/**
 * A cached instance of a persistent class
 *
 * @author Gavin King
 */
public final class CacheEntry implements Serializable {

	private final Serializable[] disassembledState;
	private final String subclass;
	private final boolean lazyPropertiesAreUnfetched;
	private final Object version;
	
	public String getSubclass() {
		return subclass;
	}
	
	public boolean areLazyPropertiesUnfetched() {
		return lazyPropertiesAreUnfetched;
	}
	
	public CacheEntry(
			final Object[] state, 
			final EntityPersister persister, 
			final boolean unfetched, 
			final Object version,
			final SessionImplementor session, 
			final Object owner) 
	throws HibernateException {
		//disassembled state gets put in a new array (we write to cache by value!)
		this.disassembledState = TypeFactory.disassemble( 
				state, 
				persister.getPropertyTypes(), 
				persister.isLazyPropertiesCacheable() ? 
					null : persister.getPropertyLaziness(),
				session, 
				owner 
			);
		subclass = persister.getEntityName();
		lazyPropertiesAreUnfetched = unfetched || !persister.isLazyPropertiesCacheable();
		this.version = version;
	}
	
	public Object getVersion() {
		return version;
	}

	CacheEntry(Serializable[] state, String subclass, boolean unfetched, Object version) {
		this.disassembledState = state;
		this.subclass = subclass;
		this.lazyPropertiesAreUnfetched = unfetched;
		this.version = version;
	}

	public Object[] assemble(
			final Object instance, 
			final Serializable id, 
			final EntityPersister persister, 
			final Interceptor interceptor, 
			final EventSource session) 
	throws HibernateException {

		if ( !persister.getEntityName().equals(subclass) ) {
			throw new AssertionFailure("Tried to assemble a different subclass instance");
		}

		return assemble(disassembledState, instance, id, persister, interceptor, session);

	}

	private static Object[] assemble(
			final Serializable[] values, 
			final Object result, 
			final Serializable id, 
			final EntityPersister persister, 
			final Interceptor interceptor, 
			final EventSource session) 
	throws HibernateException {
			
		//assembled state gets put in a new array (we read from cache by value!)
		Object[] assembledProps = TypeFactory.assemble( 
				values, 
				persister.getPropertyTypes(), 
				session, result 
			);

		//persister.setIdentifier(result, id); //before calling interceptor, for consistency with normal load

		//TODO: reuse the PreLoadEvent
		PreLoadEvent preLoadEvent = new PreLoadEvent( session )
				.setEntity(result)
				.setState(assembledProps)
				.setId(id)
				.setPersister(persister);
		
		PreLoadEventListener[] listeners = session.getListeners().getPreLoadEventListeners();
		for ( int i = 0; i < listeners.length; i++ ) {
			listeners[i].onPreLoad(preLoadEvent);
		}
		
		persister.setPropertyValues( 
				result, 
				assembledProps, 
				session.getEntityMode() 
			);

		return assembledProps;
	}

    public Serializable[] getDisassembledState() {
	    // todo: this was added to support initializing an entity's EntityEntry snapshot during reattach;
	    // this should be refactored to instead expose a method to assemble a EntityEntry based on this
	    // state for return.
	    return disassembledState;
    }

	public String toString() {
		return "CacheEntry(" + subclass + ')' + 
				ArrayHelper.toString(disassembledState);
	}

}







/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.entry;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Standard representation of entity cached data using the "disassembled state".
 *
 * @author Steve Ebersole
 */
public class StandardCacheEntryImpl implements CacheEntry {

	private final Serializable[] disassembledState;
	private final Object version;
	private final String subclass;

	/**
	 * Constructs a StandardCacheEntryImpl
	 *
	 * @param state The extracted state
	 * @param persister The entity persister
	 * @param version The current version (if versioned)
	 * @param session The originating session
	 * @param owner The owner
	 *
	 * @throws HibernateException Generally indicates a problem performing the dis-assembly.
	 */
	public StandardCacheEntryImpl(
			final Object[] state,
			final EntityPersister persister,
			final Object version,
			final SharedSessionContractImplementor session,
			final Object owner) throws HibernateException {
		// disassembled state gets put in a new array (we write to cache by value!)
		this.disassembledState = CacheEntryHelper.disassemble(
				state,
				persister.getPropertyTypes(),
				persister.isLazyPropertiesCacheable() ? null : persister.getPropertyLaziness(),
				session,
				owner
		);
		this.subclass = persister.getEntityName();
		this.version = version;
	}

	StandardCacheEntryImpl(Serializable[] disassembledState, String subclass, Object version) {
		this.disassembledState = disassembledState;
		this.subclass = subclass;
		this.version = version;
	}



	@Override
	public boolean isReferenceEntry() {
		return false;
	}

	@Override
	public Serializable[] getDisassembledState() {
		// todo: this was added to support initializing an entity's EntityEntry snapshot during reattach;
		// this should be refactored to instead expose a method to assemble an EntityEntry based on this
		// state for return.
		return disassembledState;
	}

	@Override
	public String getSubclass() {
		return subclass;
	}

	@Override
	public Object getVersion() {
		return version;
	}

	/**
	 * After assembly, is a copy of the array needed?
	 *
	 * @return true/false
	 */
	public boolean isDeepCopyNeeded() {
		// for now always return true.
		// todo : See discussion on HHH-7872
		return true;
	}

	/**
	 * Assemble the previously disassembled state represented by this entry into the given entity instance.
	 *
	 * Additionally manages the PreLoadEvent callbacks.
	 *
	 * @param instance The entity instance
	 * @param id The entity identifier
	 * @param persister The entity persister
	 * @param interceptor (currently unused)
	 * @param session The session
	 *
	 * @return The assembled state
	 *
	 * @throws HibernateException Indicates a problem performing assembly or calling the PreLoadEventListeners.
	 *
	 * @see org.hibernate.type.Type#assemble
	 * @see org.hibernate.type.Type#disassemble
	 */
	public Object[] assemble(
			final Object instance,
			final Object id,
			final EntityPersister persister,
			final Interceptor interceptor,
			final SharedSessionContractImplementor session) throws HibernateException {
		if ( !persister.getEntityName().equals( subclass ) ) {
			throw new AssertionFailure( "Tried to assemble a different subclass instance" );
		}

		// assembled state gets put in a new array (we read from cache by value!)
		final Object[] state = CacheEntryHelper.assemble(
				disassembledState,
				persister.getPropertyTypes(),
				session, instance
		);

		//persister.setIdentifier(instance, id); //before calling interceptor, for consistency with normal load

		if ( session instanceof EventSource eventSource ) {
			//TODO: reuse the PreLoadEvent
			final PreLoadEvent preLoadEvent =
					new PreLoadEvent( eventSource )
							.setEntity( instance )
							.setState( state )
							.setId( id )
							.setPersister( persister );
			session.getFactory()
					.getEventListenerGroups()
					.eventListenerGroup_PRE_LOAD
					.fireEventOnEachListener( preLoadEvent, PreLoadEventListener::onPreLoad );

		}

		persister.setPropertyValues( instance, state );

		return state;
	}

	@Override
	public String toString() {
		return "CacheEntry(" + subclass + ')';
	}

}

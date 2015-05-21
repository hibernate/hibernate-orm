/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi.entry;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.bytecode.instrumentation.internal.FieldInterceptionHelper;
import org.hibernate.bytecode.instrumentation.spi.FieldInterceptor;
import org.hibernate.bytecode.instrumentation.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.TypeHelper;

/**
 * Standard representation of entity cached data using the "disassembled state".
 *
 * @author Steve Ebersole
 */
public class StandardCacheEntryImpl implements CacheEntry {
	private final Serializable[] disassembledState;
	private final String subclass;
	private final boolean lazyPropertiesAreUnfetched;
	private final Object version;

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
			final SessionImplementor session,
			final Object owner)
			throws HibernateException {
		// disassembled state gets put in a new array (we write to cache by value!)
		this.disassembledState = TypeHelper.disassemble(
				state,
				persister.getPropertyTypes(),
				persister.isLazyPropertiesCacheable() ? null : persister.getPropertyLaziness(),
				session,
				owner
		);
		subclass = persister.getEntityName();
		lazyPropertiesAreUnfetched = arelazyPropertiesUnfetched(persister, owner, state) || !persister.isLazyPropertiesCacheable();
		this.version = version;
	}

	StandardCacheEntryImpl(Serializable[] state, String subclass, boolean unfetched, Object version) {
		this.disassembledState = state;
		this.subclass = subclass;
		this.lazyPropertiesAreUnfetched = unfetched;
		this.version = version;
	}



	@Override
	public boolean isReferenceEntry() {
		return false;
	}

	@Override
	public Serializable[] getDisassembledState() {
		// todo: this was added to support initializing an entity's EntityEntry snapshot during reattach;
		// this should be refactored to instead expose a method to assemble a EntityEntry based on this
		// state for return.
		return disassembledState;
	}

	@Override
	public String getSubclass() {
		return subclass;
	}

	@Override
	public boolean areLazyPropertiesUnfetched() {
		return lazyPropertiesAreUnfetched;
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
			final Serializable id,
			final EntityPersister persister,
			final Interceptor interceptor,
			final EventSource session) throws HibernateException {
		if ( !persister.getEntityName().equals( subclass ) ) {
			throw new AssertionFailure( "Tried to assemble a different subclass instance" );
		}

		//assembled state gets put in a new array (we read from cache by value!)
		final Object[] assembledProps = TypeHelper.assemble(
				disassembledState,
				persister.getPropertyTypes(),
				session, instance
		);

		//persister.setIdentifier(instance, id); //before calling interceptor, for consistency with normal load

		//TODO: reuse the PreLoadEvent
		final PreLoadEvent preLoadEvent = new PreLoadEvent( session )
				.setEntity( instance )
				.setState( assembledProps )
				.setId( id )
				.setPersister( persister );

		final EventListenerGroup<PreLoadEventListener> listenerGroup = session
				.getFactory()
				.getServiceRegistry()
				.getService( EventListenerRegistry.class )
				.getEventListenerGroup( EventType.PRE_LOAD );
		for ( PreLoadEventListener listener : listenerGroup.listeners() ) {
			listener.onPreLoad( preLoadEvent );
		}

		persister.setPropertyValues( instance, assembledProps );

		return assembledProps;
	}

	@Override
	public String toString() {
		return "CacheEntry(" + subclass + ')' + ArrayHelper.toString( disassembledState );
	}

	/**
	 * Does the given instance or related state have any uninitialized lazy properties.
	 * @param persister The entity persister from with we can read the meta modal and determine if the entity has
	 *                  lazy properties.
	 * @param entity The entity to be check for uninitialized lazy properties.
	 * @param entityState The entity state array, which we check for lazy properties if the entity has not yet had the
	 *                    FieldInterceptor injected.
	 * @return True if uninitialized lazy properties were found; false otherwise.
	 */
	protected boolean arelazyPropertiesUnfetched(EntityPersister persister, Object entity, Object[] entityState) {
		if (persister.getEntityMetamodel().hasLazyProperties()) {
			FieldInterceptor callback = FieldInterceptionHelper.extractFieldInterceptor(entity);
			if (callback == null) {
				//We need to check the state array and see if it has any un-fetched properties.
				for (Object property : entityState) {
					if (property == LazyPropertyInitializer.UNFETCHED_PROPERTY) {
						return true;
					}
				}
			}
			else {
				return !callback.isInitialized();
			}
		}
		return false;
	}
}

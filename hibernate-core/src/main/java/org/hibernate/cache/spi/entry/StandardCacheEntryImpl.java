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
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.internal.TypeHelper;

/**
 * Standard representation of entity cached data using the "disassembled state".
 *
 * @author Steve Ebersole
 */
public class StandardCacheEntryImpl implements CacheEntry {
	private final Serializable[] disassembledState;
	private final String disassembledStateText;
	private final Object version;
	private final String subclass;

	/**
	 * Constructs a StandardCacheEntryImpl
	 *
	 * @param state The extracted state
	 * @param descriptor The entity descriptor
	 * @param version The current version (if versioned)
	 * @param session The originating session
	 * @param owner The owner
	 *
	 * @throws HibernateException Generally indicates a problem performing the dis-assembly.
	 */
	public StandardCacheEntryImpl(
			final Object[] state,
			final EntityDescriptor descriptor,
			final Object version,
			final SharedSessionContractImplementor session,
			final Object owner) throws HibernateException {
		// disassembled state gets put in a new array (we write to cache by value!)
		this.disassembledState = new Serializable[state.length];
		descriptor.visitAttributes( new TypeHelper.FilteredAttributeConsumer() {
			final boolean[] nonCacheable = descriptor.isLazyPropertiesCacheable() ?
					null :
					descriptor.getPropertyLaziness();
			int position = 0;

			@Override
			protected void acceptAttribute(PersistentAttribute attribute) {
				if ( nonCacheable != null && nonCacheable[position] ) {
					StandardCacheEntryImpl.this.disassembledState[position] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
				}
				else if ( state[position] == LazyPropertyInitializer.UNFETCHED_PROPERTY || state[position] == PropertyAccessStrategyBackRefImpl.UNKNOWN ) {
					StandardCacheEntryImpl.this.disassembledState[position] = (Serializable) state[position];
				}
				else {
					StandardCacheEntryImpl.this.disassembledState[position] = attribute.getJavaTypeDescriptor()
							.getMutabilityPlan()
							.disassemble( state[position] );
				}
				position++;
			}
		} );
		this.disassembledStateText = TypeHelper.toLoggableString( state, descriptor );
		this.subclass = descriptor.getEntityName();
		this.version = version;
	}

	StandardCacheEntryImpl(Serializable[] state, String disassembledStateText, String subclass, Object version) {
		this.disassembledState = state;
		this.disassembledStateText = disassembledStateText;
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
		// this should be refactored to instead expose a method to assemble a EntityEntry based on this
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
	 * @param descriptor The entity descriptor
	 * @param interceptor (currently unused)
	 * @param session The session
	 *
	 * @return The assembled state
	 *
	 * @throws HibernateException Indicates a problem performing assembly or calling the PreLoadEventListeners.
	 *
	 * @see MutabilityPlan#assemble
	 * @see MutabilityPlan#disassemble
	 */
	public Object[] assemble(
			final Object instance,
			final Serializable id,
			final EntityDescriptor descriptor,
			final Interceptor interceptor,
			final EventSource session) throws HibernateException {
		if ( !descriptor.getEntityName().equals( subclass ) ) {
			throw new AssertionFailure( "Tried to assemble a different subclass instance" );
		}

		//assembled state gets put in a new array (we read from cache by value!)
		final Object[] assembledProps = TypeHelper.assemble(
				disassembledState,
				descriptor.getPropertyTypes(),
				session, instance
		);

		//descriptor.setIdentifier(instance, id); //beforeQuery calling interceptor, for consistency with normal load

		//TODO: reuse the PreLoadEvent
		final PreLoadEvent preLoadEvent = new PreLoadEvent( session )
				.setEntity( instance )
				.setState( assembledProps )
				.setId( id )
				.setPersister( descriptor );

		final EventListenerGroup<PreLoadEventListener> listenerGroup = session
				.getFactory()
				.getServiceRegistry()
				.getService( EventListenerRegistry.class )
				.getEventListenerGroup( EventType.PRE_LOAD );
		for ( PreLoadEventListener listener : listenerGroup.listeners() ) {
			listener.onPreLoad( preLoadEvent );
		}

		descriptor.setPropertyValues( instance, assembledProps );

		return assembledProps;
	}

	@Override
	public String toString() {
		return "CacheEntry(" + subclass + " {" + disassembledStateText + "})";
	}
}

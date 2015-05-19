/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.internal.core;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.internal.DefaultFlushEntityEventListener;
import org.hibernate.jpa.event.internal.jpa.CallbackRegistryConsumer;
import org.hibernate.jpa.event.spi.jpa.CallbackRegistry;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

/**
 * Overrides the LifeCycle OnSave call to call the PreUpdate operation
 *
 * @author Emmanuel Bernard
 */
public class JpaFlushEntityEventListener extends DefaultFlushEntityEventListener implements CallbackRegistryConsumer {
	private CallbackRegistry callbackRegistry;

	public void injectCallbackRegistry(CallbackRegistry callbackRegistry) {
		this.callbackRegistry = callbackRegistry;
	}

	public JpaFlushEntityEventListener() {
		super();
	}

	public JpaFlushEntityEventListener(CallbackRegistry callbackRegistry) {
		super();
		this.callbackRegistry = callbackRegistry;
	}

	@Override
	protected boolean invokeInterceptor(
			SessionImplementor session,
			Object entity,
			EntityEntry entry,
			Object[] values,
			EntityPersister persister) {
		boolean isDirty = false;
		if ( entry.getStatus() != Status.DELETED ) {
			if ( callbackRegistry.preUpdate( entity ) ) {
				isDirty = copyState( entity, persister.getPropertyTypes(), values, session.getFactory() );
			}
		}
		return super.invokeInterceptor( session, entity, entry, values, persister ) || isDirty;
	}

	private boolean copyState(Object entity, Type[] types, Object[] state, SessionFactory sf) {
		// copy the entity state into the state array and return true if the state has changed
		ClassMetadata metadata = sf.getClassMetadata( entity.getClass() );
		Object[] newState = metadata.getPropertyValues( entity );
		int size = newState.length;
		boolean isDirty = false;
		for ( int index = 0; index < size ; index++ ) {
			if ( !types[index].isEqual( state[index], newState[index] ) ) {
				isDirty = true;
				state[index] = newState[index];
			}
		}
		return isDirty;
	}
}

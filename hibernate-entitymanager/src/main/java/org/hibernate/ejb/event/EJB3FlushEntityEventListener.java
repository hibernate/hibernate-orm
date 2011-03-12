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

import org.hibernate.EntityMode;
import org.hibernate.SessionFactory;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.Status;
import org.hibernate.event.def.DefaultFlushEntityEventListener;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

/**
 * Overrides the LifeCycle OnSave call to call the PreUpdate operation
 *
 * @author Emmanuel Bernard
 */
public class EJB3FlushEntityEventListener extends DefaultFlushEntityEventListener implements CallbackHandlerConsumer {
	private EntityCallbackHandler callbackHandler;

	public void setCallbackHandler(EntityCallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	public EJB3FlushEntityEventListener() {
		super();
	}

	public EJB3FlushEntityEventListener(EntityCallbackHandler callbackHandler) {
		super();
		this.callbackHandler = callbackHandler;
	}

	@Override
	protected boolean invokeInterceptor(
			SessionImplementor session, Object entity, EntityEntry entry, Object[] values, EntityPersister persister
	) {
		boolean isDirty = false;
		if ( entry.getStatus() != Status.DELETED ) {
			if ( callbackHandler.preUpdate( entity ) ) {
				isDirty = copyState( entity, persister.getPropertyTypes(), values, session.getFactory() );
			}
		}
		return super.invokeInterceptor( session, entity, entry, values, persister ) || isDirty;
	}

	/**
	 * copy the entity state into the state array and return true if the state has changed
	 */
	private boolean copyState(Object entity, Type[] types, Object[] state, SessionFactory sf) {
		ClassMetadata metadata = sf.getClassMetadata( entity.getClass() );
		Object[] newState = metadata.getPropertyValues( entity, EntityMode.POJO );
		int size = newState.length;
		boolean isDirty = false;
		for ( int index = 0; index < size ; index++ ) {
			if ( !types[index].isEqual( state[index], newState[index], EntityMode.POJO ) ) {
				isDirty = true;
				state[index] = newState[index];
			}
		}
		return isDirty;
	}
}

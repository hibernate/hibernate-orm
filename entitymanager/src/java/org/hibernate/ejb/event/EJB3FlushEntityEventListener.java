//$Id$
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

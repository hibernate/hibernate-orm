package org.hibernate.intercept;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.LazyInitializationException;

import java.util.Set;
import java.io.Serializable;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractFieldInterceptor implements FieldInterceptor, Serializable {

	private transient SessionImplementor session;
	private Set uninitializedFields;
	private final String entityName;

	private transient boolean initializing;
	private boolean dirty;

	protected AbstractFieldInterceptor(SessionImplementor session, Set uninitializedFields, String entityName) {
		this.session = session;
		this.uninitializedFields = uninitializedFields;
		this.entityName = entityName;
	}


	// FieldInterceptor impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public final void setSession(SessionImplementor session) {
		this.session = session;
	}

	public final boolean isInitialized() {
		return uninitializedFields == null || uninitializedFields.size() == 0;
	}

	public final boolean isInitialized(String field) {
		return uninitializedFields == null || !uninitializedFields.contains( field );
	}

	public final void dirty() {
		dirty = true;
	}

	public final boolean isDirty() {
		return dirty;
	}

	public final void clearDirty() {
		dirty = false;
	}


	// subclass accesses ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected final Object intercept(Object target, String fieldName, Object value) {
		if ( initializing ) {
			return value;
		}

		if ( uninitializedFields != null && uninitializedFields.contains( fieldName ) ) {
			if ( session == null ) {
				throw new LazyInitializationException( "entity with lazy properties is not associated with a session" );
			}
			else if ( !session.isOpen() || !session.isConnected() ) {
				throw new LazyInitializationException( "session is not connected" );
			}

			final Object result;
			initializing = true;
			try {
				result = ( ( LazyPropertyInitializer ) session.getFactory()
						.getEntityPersister( entityName ) )
						.initializeLazyProperty( fieldName, target, session );
			}
			finally {
				initializing = false;
			}
			uninitializedFields = null; //let's assume that there is only one lazy fetch group, for now!
			return result;
		}
		else {
			return value;
		}
	}

	public final SessionImplementor getSession() {
		return session;
	}

	public final Set getUninitializedFields() {
		return uninitializedFields;
	}

	public final String getEntityName() {
		return entityName;
	}

	public final boolean isInitializing() {
		return initializing;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.instrumentation.spi;
import java.io.Serializable;
import java.util.Set;

import org.hibernate.LazyInitializationException;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Base support for FieldInterceptor implementations.
 *
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

	@Override
	public final void setSession(SessionImplementor session) {
		this.session = session;
	}

	@Override
	public final boolean isInitialized() {
		return uninitializedFields == null || uninitializedFields.size() == 0;
	}

	@Override
	public final boolean isInitialized(String field) {
		return uninitializedFields == null || !uninitializedFields.contains( field );
	}

	@Override
	public final void dirty() {
		dirty = true;
	}

	@Override
	public final boolean isDirty() {
		return dirty;
	}

	@Override
	public final void clearDirty() {
		dirty = false;
	}


	// subclass accesses ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Interception of access to the named field
	 *
	 * @param target The call target
	 * @param fieldName The name of the field.
	 * @param value The value.
	 *
	 * @return ?
	 */
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
				result = ( (LazyPropertyInitializer) session.getFactory().getEntityPersister( entityName ) )
						.initializeLazyProperty( fieldName, target, session );
			}
			finally {
				initializing = false;
			}
			// let's assume that there is only one lazy fetch group, for now!
			uninitializedFields = null;
			return result;
		}
		else {
			return value;
		}
	}

	/**
	 * Access to the session
	 *
	 * @return The associated session
	 */
	public final SessionImplementor getSession() {
		return session;
	}

	/**
	 * Access to all currently uninitialized fields
	 *
	 * @return The name of all currently uninitialized fields
	 */
	public final Set getUninitializedFields() {
		return uninitializedFields;
	}

	/**
	 * Access to the intercepted entity name
	 *
	 * @return The entity name
	 */
	public final String getEntityName() {
		return entityName;
	}

	/**
	 * Is the instance currently initializing?
	 *
	 * @return true/false.
	 */
	public final boolean isInitializing() {
		return initializing;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.bytecode.enhance.spi.interceptor;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.bytecode.enhance.spi.CollectionTracker;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.InterceptorImplementor;
import org.hibernate.bytecode.enhance.spi.interceptor.Helper.Consumer;
import org.hibernate.bytecode.enhance.spi.interceptor.Helper.LazyInitializationWork;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.persister.entity.EntityPersister;

import org.jboss.logging.Logger;

/**
 * Interceptor that loads attributes lazily
 *
 * @author Luis Barreiro
 * @author Steve Ebersole
 */
public class LazyAttributeLoadingInterceptor
		implements PersistentAttributeInterceptor, Consumer, InterceptorImplementor {
	private static final Logger log = Logger.getLogger( LazyAttributeLoadingInterceptor.class );

	private final String entityName;
	private final Set<String> lazyFields;

	private Set<String> initializedLazyFields;

	private transient SharedSessionContractImplementor session;
	private boolean allowLoadOutsideTransaction;
	private String sessionFactoryUuid;

	public LazyAttributeLoadingInterceptor(
			String entityName,
			Set<String> lazyFields,
			SharedSessionContractImplementor session) {
		this.entityName = entityName;
		this.lazyFields = lazyFields;

		setSession( session );
	}

	protected final Object intercept(Object target, String attributeName, Object value) {
		if ( !isAttributeLoaded( attributeName ) ) {
			Object loadedValue = fetchAttribute( target, attributeName );
			attributeInitialized( attributeName );
			return loadedValue;
		}
		return value;
	}

	/**
	 * Fetches the lazy attribute. The attribute does not get associated with the entity. (To be used by hibernate methods)
	 */
	public Object fetchAttribute(final Object target, final String attributeName) {
		return loadAttribute( target, attributeName );
	}

	protected Object loadAttribute(final Object target, final String attributeName) {
		return new Helper( this ).performWork(
				new LazyInitializationWork() {
					@Override
					public Object doWork(SharedSessionContractImplementor session, boolean isTemporarySession) {
						final EntityPersister persister = session.getFactory().getMetamodel().entityPersister( getEntityName() );

						if ( isTemporarySession ) {
							final Serializable id = persister.getIdentifier( target, null );

							// Add an entry for this entity in the PC of the temp Session
							// NOTE : a few arguments that would be nice to pass along here...
							//		1) loadedState if we know any
							final Object[] loadedState = null;
							//		2) does a row exist in the db for this entity?
							final boolean existsInDb = true;
							session.getPersistenceContext().addEntity(
									target,
									Status.READ_ONLY,
									loadedState,
									session.generateEntityKey( id, persister ),
									persister.getVersion( target ),
									LockMode.NONE,
									existsInDb,
									persister,
									true
							);
						}

						final LazyPropertyInitializer initializer = (LazyPropertyInitializer) persister;
						final Object loadedValue = initializer.initializeLazyProperty(
								attributeName,
								target,
								session
						);

						takeCollectionSizeSnapshot( target, attributeName, loadedValue );
						return loadedValue;
					}

					@Override
					public String getEntityName() {
						return entityName;
					}

					@Override
					public String getAttributeName() {
						return attributeName;
					}
				}
		);
	}

	public final void setSession(SharedSessionContractImplementor session) {
		this.session = session;
		if ( session != null && !allowLoadOutsideTransaction ) {
			this.allowLoadOutsideTransaction = session.getFactory().getSessionFactoryOptions().isInitializeLazyStateOutsideTransactionsEnabled();
			if ( this.allowLoadOutsideTransaction ) {
				this.sessionFactoryUuid = session.getFactory().getUuid();
			}
		}
	}

	public final void unsetSession() {
		this.session = null;
	}

	public boolean isAttributeLoaded(String fieldName) {
		return !isLazyAttribute( fieldName ) || isInitializedLazyField( fieldName );
	}

	private boolean isLazyAttribute(String fieldName) {
		return lazyFields == null || lazyFields.contains( fieldName );
	}

	private boolean isInitializedLazyField(String fieldName) {
		return initializedLazyFields != null && initializedLazyFields.contains( fieldName );
	}

	public boolean hasAnyUninitializedAttributes() {
		if ( lazyFields == null ) {
			return false;
		}

		if ( initializedLazyFields == null ) {
			return true;
		}

		for ( String fieldName : lazyFields ) {
			if ( !initializedLazyFields.contains( fieldName ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String toString() {
		return "LazyAttributeLoader(entityName=" + entityName + " ,lazyFields=" + lazyFields + ')';
	}

	//

	private void takeCollectionSizeSnapshot(Object target, String fieldName, Object value) {
		if ( value != null && value instanceof Collection && target instanceof SelfDirtinessTracker ) {
			CollectionTracker tracker = ( (SelfDirtinessTracker) target ).$$_hibernate_getCollectionTracker();
			if ( tracker == null ) {
				( (SelfDirtinessTracker) target ).$$_hibernate_clearDirtyAttributes();
				tracker = ( (SelfDirtinessTracker) target ).$$_hibernate_getCollectionTracker();
			}
			tracker.add( fieldName, ( (Collection) value ).size() );
		}
	}

	@Override
	public boolean readBoolean(Object obj, String name, boolean oldValue) {
		return (Boolean) intercept( obj, name, oldValue );
	}

	@Override
	public boolean writeBoolean(Object obj, String name, boolean oldValue, boolean newValue) {
		if ( lazyFields != null && lazyFields.contains( name ) ) {
			attributeInitialized( name );
		}
		return newValue;
	}

	@Override
	public byte readByte(Object obj, String name, byte oldValue) {
		return (Byte) intercept( obj, name, oldValue );
	}

	@Override
	public byte writeByte(Object obj, String name, byte oldValue, byte newValue) {
		if ( lazyFields != null && lazyFields.contains( name ) ) {
			attributeInitialized( name );
		}
		return newValue;
	}

	@Override
	public char readChar(Object obj, String name, char oldValue) {
		return (Character) intercept( obj, name, oldValue );
	}

	@Override
	public char writeChar(Object obj, String name, char oldValue, char newValue) {
		if ( lazyFields != null && lazyFields.contains( name ) ) {
			attributeInitialized( name );
		}
		return newValue;
	}

	@Override
	public short readShort(Object obj, String name, short oldValue) {
		return (Short) intercept( obj, name, oldValue );
	}

	@Override
	public short writeShort(Object obj, String name, short oldValue, short newValue) {
		if ( lazyFields != null && lazyFields.contains( name ) ) {
			attributeInitialized( name );
		}
		return newValue;
	}

	@Override
	public int readInt(Object obj, String name, int oldValue) {
		return (Integer) intercept( obj, name, oldValue );
	}

	@Override
	public int writeInt(Object obj, String name, int oldValue, int newValue) {
		if ( lazyFields != null && lazyFields.contains( name ) ) {
			attributeInitialized( name );
		}
		return newValue;
	}

	@Override
	public float readFloat(Object obj, String name, float oldValue) {
		return (Float) intercept( obj, name, oldValue );
	}

	@Override
	public float writeFloat(Object obj, String name, float oldValue, float newValue) {
		if ( lazyFields != null && lazyFields.contains( name ) ) {
			attributeInitialized( name );
		}
		return newValue;
	}

	@Override
	public double readDouble(Object obj, String name, double oldValue) {
		return (Double) intercept( obj, name, oldValue );
	}

	@Override
	public double writeDouble(Object obj, String name, double oldValue, double newValue) {
		if ( lazyFields != null && lazyFields.contains( name ) ) {
			attributeInitialized( name );
		}
		return newValue;
	}

	@Override
	public long readLong(Object obj, String name, long oldValue) {
		return (Long) intercept( obj, name, oldValue );
	}

	@Override
	public long writeLong(Object obj, String name, long oldValue, long newValue) {
		if ( lazyFields != null && lazyFields.contains( name ) ) {
			attributeInitialized( name );
		}
		return newValue;
	}

	@Override
	public Object readObject(Object obj, String name, Object oldValue) {
		return intercept( obj, name, oldValue );
	}

	@Override
	public Object writeObject(Object obj, String name, Object oldValue, Object newValue) {
		if ( lazyFields != null && lazyFields.contains( name ) ) {
			attributeInitialized( name );
		}
		return newValue;
	}

	@Override
	public SharedSessionContractImplementor getLinkedSession() {
		return session;
	}

	@Override
	public boolean allowLoadOutsideTransaction() {
		return allowLoadOutsideTransaction;
	}

	@Override
	public String getSessionFactoryUuid() {
		return sessionFactoryUuid;
	}

	@Override
	public void attributeInitialized(String name) {
		if ( !isLazyAttribute( name ) ) {
			return;
		}
		if ( initializedLazyFields == null ) {
			initializedLazyFields = new HashSet<String>();
		}
		initializedLazyFields.add( name );
	}

	@Override
	public Set<String> getInitializedLazyAttributeNames() {
		return initializedLazyFields == null ? Collections.<String>emptySet() : initializedLazyFields;
	}

}

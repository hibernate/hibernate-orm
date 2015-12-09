/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.bytecode.enhance.spi.interceptor;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import javax.naming.NamingException;

import org.hibernate.LockMode;
import org.hibernate.bytecode.enhance.internal.tracker.SimpleFieldTracker;
import org.hibernate.bytecode.enhance.spi.CollectionTracker;
import org.hibernate.bytecode.enhance.spi.interceptor.Helper.Consumer;
import org.hibernate.bytecode.enhance.spi.interceptor.Helper.LazyInitializationWork;
import org.hibernate.bytecode.instrumentation.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.persister.entity.EntityPersister;

import org.jboss.logging.Logger;

/**
 * Interceptor that loads attributes lazily
 *
 * @author Luis Barreiro
 */
public class LazyAttributeLoader implements PersistentAttributeInterceptor, Consumer {
	private static final Logger log = Logger.getLogger( LazyAttributeLoader.class );

	private transient SessionImplementor session;

	private final Set<String> lazyFields;
	private final String entityName;

	private String sessionFactoryUuid;
	private boolean allowLoadOutsideTransaction;

	private final SimpleFieldTracker initializedFields = new SimpleFieldTracker();

	public LazyAttributeLoader(SessionImplementor session, Set<String> lazyFields, String entityName) {
		this.session = session;
		this.lazyFields = lazyFields;
		this.entityName = entityName;

		this.allowLoadOutsideTransaction = session.getFactory().getSessionFactoryOptions().isInitializeLazyStateOutsideTransactionsEnabled();
		if ( this.allowLoadOutsideTransaction ) {
			try {
				this.sessionFactoryUuid = (String) session.getFactory().getReference().get( "uuid" ).getContent();
			}
			catch (NamingException e) {
				log.debug( "Unable to determine SF UUID in preparation for `allowLoadOutsideTransaction`" );
			}
		}
	}

	protected final Object intercept(Object target, String attributeName, Object value) {
		if ( !isAttributeLoaded( attributeName ) ) {
			Object loadedValue = fetchAttribute( target, attributeName );
			setLoaded( attributeName );
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
					public Object doWork(SessionImplementor session, boolean isTemporarySession) {
						final EntityPersister persister = session.getFactory().getEntityPersister( getEntityName() );

						if ( isTemporarySession ) {
							final Serializable id = persister.getIdentifier( target, null );

							// Add an entry for this entity in the PC of the temp Session
							// NOTE : a few arguments that would be nice to pass along here...
							//		1) loadedState if we know any
							final Object[] loadedState = null;
							//		2) does a row exist in the db for this entity?
							final boolean existsInDb = true;
							// NOTE2: the final boolean is 'lazyPropertiesAreUnfetched' which is another
							//	place where a "single lazy fetch group" shows up
							session.getPersistenceContext().addEntity(
									target,
									Status.READ_ONLY,
									loadedState,
									session.generateEntityKey( id, persister ),
									persister.getVersion( target ),
									LockMode.NONE,
									existsInDb,
									persister,
									true,
									true
							);
						}

						final LazyPropertyInitializer initializer = (LazyPropertyInitializer) persister;
						final Object loadedValue = initializer.initializeLazyProperty(
								attributeName,
								target,
								session
						);

						initializedFields.add( attributeName );
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

	public final void setSession(SessionImplementor session) {
		this.session = session;
	}

	public final void unsetSession() {
		this.session = null;
	}

	public boolean isAttributeLoaded(String fieldName) {
		return lazyFields == null || !lazyFields.contains( fieldName ) || initializedFields.contains( fieldName );
	}

	public boolean isUninitialized() {
		if ( lazyFields != null ) {
			for ( String fieldName : lazyFields ) {
				if ( !initializedFields.contains( fieldName ) ) {
					return true;
				}
			}
		}
		return false;
	}

	public void setLoaded(String attributeName) {
		initializedFields.add( attributeName );
	}

	public String[] getiInitializedFields() {
		return initializedFields.get();
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
			initializedFields.add( name );
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
			initializedFields.add( name );
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
			initializedFields.add( name );
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
			initializedFields.add( name );
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
			initializedFields.add( name );
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
			initializedFields.add( name );
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
			initializedFields.add( name );
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
			initializedFields.add( name );
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
			initializedFields.add( name );
		}
		return newValue;
	}

	@Override
	public SessionImplementor getLinkedSession() {
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
}

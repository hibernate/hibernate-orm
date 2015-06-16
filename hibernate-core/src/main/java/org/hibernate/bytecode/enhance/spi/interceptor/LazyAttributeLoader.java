/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.bytecode.enhance.spi.interceptor;

import java.util.Set;

import org.hibernate.LazyInitializationException;
import org.hibernate.bytecode.enhance.internal.tracker.SimpleDirtyTracker;
import org.hibernate.bytecode.instrumentation.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Interceptor that loads attributes lazily
 *
 * @author Luis Barreiro
 */
public class LazyAttributeLoader implements PersistentAttributeInterceptor {

	private final transient SessionImplementor session;
	private final Set<String> lazyFields;
	private final String entityName;

	private final SimpleDirtyTracker initializedFields = new SimpleDirtyTracker();

	public LazyAttributeLoader(SessionImplementor session, Set<String> lazyFields, String entityName) {
		this.session = session;
		this.lazyFields = lazyFields;
		this.entityName = entityName;
	}

	protected final Object intercept(Object target, String fieldName, Object value) {
		if ( lazyFields != null && lazyFields.contains( fieldName ) && !initializedFields.contains( fieldName ) ) {
			if ( session == null ) {
				throw new LazyInitializationException( "entity with lazy properties is not associated with a session" );
			}
			else if ( !session.isOpen() || !session.isConnected() ) {
				throw new LazyInitializationException( "session is not connected" );
			}

			Object loadedValue = ( (LazyPropertyInitializer) session.getFactory()
					.getEntityPersister( entityName ) ).initializeLazyProperty(
					fieldName,
					target,
					session
			);

			initializedFields.add( fieldName );
			return loadedValue;
		}
		else {
			return value;
		}
	}

	@Override
	public String toString() {
		return "LazyAttributeLoader(entityName=" + entityName + " ,lazyFields=" + lazyFields + ')';
	}

	/* --- */

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
}

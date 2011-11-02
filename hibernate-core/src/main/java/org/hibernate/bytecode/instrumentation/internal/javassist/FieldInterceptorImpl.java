/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.bytecode.instrumentation.internal.javassist;

import java.io.Serializable;
import java.util.Set;

import org.hibernate.bytecode.instrumentation.spi.AbstractFieldInterceptor;
import org.hibernate.bytecode.internal.javassist.FieldHandler;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

/**
 * A field-level interceptor that initializes lazily fetched properties.
 * This interceptor can be attached to classes instrumented by Javassist.
 * Note that this implementation assumes that the instance variable
 * name is the same as the name of the persistent property that must
 * be loaded.
 * </p>
 * Note: most of the interesting functionality here is farmed off
 * to the super-class.  The stuff here mainly acts as an adapter to the
 * Javassist-specific functionality, routing interception through
 * the super-class's intercept() method
 *
 * @author Steve Ebersole
 */
@SuppressWarnings( {"UnnecessaryUnboxing", "UnnecessaryBoxing"})
public final class FieldInterceptorImpl extends AbstractFieldInterceptor implements FieldHandler, Serializable {

	FieldInterceptorImpl(SessionImplementor session, Set uninitializedFields, String entityName) {
		super( session, uninitializedFields, entityName );
	}


	// FieldHandler impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean readBoolean(Object target, String name, boolean oldValue) {
		return ( ( Boolean ) intercept( target, name, oldValue ) )
				.booleanValue();
	}

	public byte readByte(Object target, String name, byte oldValue) {
		return ( ( Byte ) intercept( target, name, Byte.valueOf( oldValue ) ) ).byteValue();
	}

	public char readChar(Object target, String name, char oldValue) {
		return ( ( Character ) intercept( target, name, Character.valueOf( oldValue ) ) )
				.charValue();
	}

	public double readDouble(Object target, String name, double oldValue) {
		return ( ( Double ) intercept( target, name, Double.valueOf( oldValue ) ) )
				.doubleValue();
	}

	public float readFloat(Object target, String name, float oldValue) {
		return ( ( Float ) intercept( target, name, Float.valueOf( oldValue ) ) )
				.floatValue();
	}

	public int readInt(Object target, String name, int oldValue) {
		return ( ( Integer ) intercept( target, name, Integer.valueOf( oldValue ) ) )
				.intValue();
	}

	public long readLong(Object target, String name, long oldValue) {
		return ( ( Long ) intercept( target, name, Long.valueOf( oldValue ) ) ).longValue();
	}

	public short readShort(Object target, String name, short oldValue) {
		return ( ( Short ) intercept( target, name, Short.valueOf( oldValue ) ) )
				.shortValue();
	}

	public Object readObject(Object target, String name, Object oldValue) {
		Object value = intercept( target, name, oldValue );
		if (value instanceof HibernateProxy) {
			LazyInitializer li = ( (HibernateProxy) value ).getHibernateLazyInitializer();
			if ( li.isUnwrap() ) {
				value = li.getImplementation();
			}
		}
		return value;
	}

	public boolean writeBoolean(Object target, String name, boolean oldValue, boolean newValue) {
		dirty();
		intercept( target, name, oldValue );
		return newValue;
	}

	public byte writeByte(Object target, String name, byte oldValue, byte newValue) {
		dirty();
		intercept( target, name, Byte.valueOf( oldValue ) );
		return newValue;
	}

	public char writeChar(Object target, String name, char oldValue, char newValue) {
		dirty();
		intercept( target, name, Character.valueOf( oldValue ) );
		return newValue;
	}

	public double writeDouble(Object target, String name, double oldValue, double newValue) {
		dirty();
		intercept( target, name, Double.valueOf( oldValue ) );
		return newValue;
	}

	public float writeFloat(Object target, String name, float oldValue, float newValue) {
		dirty();
		intercept( target, name, Float.valueOf( oldValue ) );
		return newValue;
	}

	public int writeInt(Object target, String name, int oldValue, int newValue) {
		dirty();
		intercept( target, name, Integer.valueOf( oldValue ) );
		return newValue;
	}

	public long writeLong(Object target, String name, long oldValue, long newValue) {
		dirty();
		intercept( target, name, Long.valueOf( oldValue ) );
		return newValue;
	}

	public short writeShort(Object target, String name, short oldValue, short newValue) {
		dirty();
		intercept( target, name, Short.valueOf( oldValue ) );
		return newValue;
	}

	public Object writeObject(Object target, String name, Object oldValue, Object newValue) {
		dirty();
		intercept( target, name, oldValue );
		return newValue;
	}

	public String toString() {
		return "FieldInterceptorImpl(" +
		       "entityName=" + getEntityName() +
		       ",dirty=" + isDirty() +
		       ",uninitializedFields=" + getUninitializedFields() +
		       ')';
	}

}
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.bytecode.enhancement.basic;

import java.util.Set;

import org.hibernate.engine.spi.PersistentAttributeInterceptor;

/**
 * Interceptor that stores a marker object on write, instead of the provided value.
 * Also returns another marker object on read. Marks only non-primitive fields.
 *
 * @author Luis Barreiro
 */
public class ObjectAttributeMarkerInterceptor implements PersistentAttributeInterceptor {

	public static final Object READ_MARKER = new Object();
	public static final Object WRITE_MARKER = new Object();

	public ObjectAttributeMarkerInterceptor() {
	}

	@Override
	public boolean readBoolean(Object obj, String name, boolean oldValue) {
		return oldValue;
	}

	@Override
	public boolean writeBoolean(Object obj, String name, boolean oldValue, boolean newValue) {
		return newValue;
	}

	@Override
	public byte readByte(Object obj, String name, byte oldValue) {
		return oldValue;
	}

	@Override
	public byte writeByte(Object obj, String name, byte oldValue, byte newValue) {
		return newValue;
	}

	@Override
	public char readChar(Object obj, String name, char oldValue) {
		return oldValue;
	}

	@Override
	public char writeChar(Object obj, String name, char oldValue, char newValue) {
		return newValue;
	}

	@Override
	public short readShort(Object obj, String name, short oldValue) {
		return oldValue;
	}

	@Override
	public short writeShort(Object obj, String name, short oldValue, short newValue) {
		return newValue;
	}

	@Override
	public int readInt(Object obj, String name, int oldValue) {
		return oldValue;
	}

	@Override
	public int writeInt(Object obj, String name, int oldValue, int newValue) {
		return newValue;
	}

	@Override
	public float readFloat(Object obj, String name, float oldValue) {
		return oldValue;
	}

	@Override
	public float writeFloat(Object obj, String name, float oldValue, float newValue) {
		return newValue;
	}

	@Override
	public double readDouble(Object obj, String name, double oldValue) {
		return oldValue;
	}

	@Override
	public double writeDouble(Object obj, String name, double oldValue, double newValue) {
		return newValue;
	}

	@Override
	public long readLong(Object obj, String name, long oldValue) {
		return oldValue;
	}

	@Override
	public long writeLong(Object obj, String name, long oldValue, long newValue) {
		return newValue;
	}

	@Override
	public Object readObject(Object obj, String name, Object oldValue) {
		return READ_MARKER;
	}

	@Override
	public Object writeObject(Object obj, String name, Object oldValue, Object newValue) {
		return WRITE_MARKER;
	}

	@Override
	public Set<String> getInitializedLazyAttributeNames() {
		return null;
	}

	@Override
	public void attributeInitialized(String name) {

	}
}

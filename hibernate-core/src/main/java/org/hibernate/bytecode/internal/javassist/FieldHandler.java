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
package org.hibernate.bytecode.internal.javassist;

/**
 * The interface defining how interception of a field should be handled.
 *
 * @author Muga Nishizawa
 */
public interface FieldHandler {

	/**
	 * Called to handle writing an int value to a given field.
	 *
	 * @param obj ?
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 * @param newValue The new field value.
	 * @return ?
	 */
	int writeInt(Object obj, String name, int oldValue, int newValue);

	char writeChar(Object obj, String name, char oldValue, char newValue);

	byte writeByte(Object obj, String name, byte oldValue, byte newValue);

	boolean writeBoolean(Object obj, String name, boolean oldValue,
			boolean newValue);

	short writeShort(Object obj, String name, short oldValue, short newValue);

	float writeFloat(Object obj, String name, float oldValue, float newValue);

	double writeDouble(Object obj, String name, double oldValue, double newValue);

	long writeLong(Object obj, String name, long oldValue, long newValue);

	Object writeObject(Object obj, String name, Object oldValue, Object newValue);

	int readInt(Object obj, String name, int oldValue);

	char readChar(Object obj, String name, char oldValue);

	byte readByte(Object obj, String name, byte oldValue);

	boolean readBoolean(Object obj, String name, boolean oldValue);

	short readShort(Object obj, String name, short oldValue);

	float readFloat(Object obj, String name, float oldValue);

	double readDouble(Object obj, String name, double oldValue);

	long readLong(Object obj, String name, long oldValue);

	Object readObject(Object obj, String name, Object oldValue);

}

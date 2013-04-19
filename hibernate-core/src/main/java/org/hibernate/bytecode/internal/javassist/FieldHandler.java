/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2013, Red Hat Inc. or third-party contributors as
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
@SuppressWarnings("UnusedDeclaration")
public interface FieldHandler {

	/**
	 * Called to handle writing an int value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 * @param newValue The new field value.
	 *
	 * @return The new value, typically the same as the newValue argument
	 */
	int writeInt(Object obj, String name, int oldValue, int newValue);

	/**
	 * Called to handle writing a char value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 * @param newValue The new field value.
	 *
	 * @return The new value, typically the same as the newValue argument
	 */
	char writeChar(Object obj, String name, char oldValue, char newValue);

	/**
	 * Called to handle writing a byte value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 * @param newValue The new field value.
	 *
	 * @return The new value, typically the same as the newValue argument
	 */
	byte writeByte(Object obj, String name, byte oldValue, byte newValue);

	/**
	 * Called to handle writing a boolean value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 * @param newValue The new field value.
	 *
	 * @return The new value, typically the same as the newValue argument
	 */
	boolean writeBoolean(Object obj, String name, boolean oldValue, boolean newValue);

	/**
	 * Called to handle writing a short value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 * @param newValue The new field value.
	 *
	 * @return The new value, typically the same as the newValue argument
	 */
	short writeShort(Object obj, String name, short oldValue, short newValue);

	/**
	 * Called to handle writing a float value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 * @param newValue The new field value.
	 *
	 * @return The new value, typically the same as the newValue argument
	 */
	float writeFloat(Object obj, String name, float oldValue, float newValue);

	/**
	 * Called to handle writing a double value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 * @param newValue The new field value.
	 *
	 * @return The new value, typically the same as the newValue argument
	 */
	double writeDouble(Object obj, String name, double oldValue, double newValue);

	/**
	 * Called to handle writing a long value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 * @param newValue The new field value.
	 *
	 * @return The new value, typically the same as the newValue argument
	 */
	long writeLong(Object obj, String name, long oldValue, long newValue);

	/**
	 * Called to handle writing an Object value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 * @param newValue The new field value.
	 *
	 * @return The new value, typically the same as the newValue argument; may be different for entity references
	 */
	Object writeObject(Object obj, String name, Object oldValue, Object newValue);

	/**
	 * Called to handle reading an int value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 *
	 * @return The field value
	 */
	int readInt(Object obj, String name, int oldValue);

	/**
	 * Called to handle reading a char value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 *
	 * @return The field value
	 */
	char readChar(Object obj, String name, char oldValue);

	/**
	 * Called to handle reading a byte value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 *
	 * @return The field value
	 */
	byte readByte(Object obj, String name, byte oldValue);

	/**
	 * Called to handle reading a boolean value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 *
	 * @return The field value
	 */
	boolean readBoolean(Object obj, String name, boolean oldValue);

	/**
	 * Called to handle reading a short value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 *
	 * @return The field value
	 */
	short readShort(Object obj, String name, short oldValue);

	/**
	 * Called to handle reading a float value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 *
	 * @return The field value
	 */
	float readFloat(Object obj, String name, float oldValue);

	/**
	 * Called to handle reading a double value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 *
	 * @return The field value
	 */
	double readDouble(Object obj, String name, double oldValue);

	/**
	 * Called to handle reading a long value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 *
	 * @return The field value
	 */
	long readLong(Object obj, String name, long oldValue);

	/**
	 * Called to handle reading an Object value to a given field.
	 *
	 * @param obj The object instance on which the write was invoked
	 * @param name The name of the field being written
	 * @param oldValue The old field value
	 *
	 * @return The field value
	 */
	Object readObject(Object obj, String name, Object oldValue);

}

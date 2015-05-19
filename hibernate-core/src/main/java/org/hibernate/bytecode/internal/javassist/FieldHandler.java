/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

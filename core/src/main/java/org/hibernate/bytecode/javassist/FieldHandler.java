package org.hibernate.bytecode.javassist;

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

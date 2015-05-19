/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

/**
 * @author Steve Ebersole
 */
public interface PersistentAttributeInterceptor {

	public boolean readBoolean(Object obj, String name, boolean oldValue);

	public boolean writeBoolean(Object obj, String name, boolean oldValue, boolean newValue);

	public byte readByte(Object obj, String name, byte oldValue);

	public byte writeByte(Object obj, String name, byte oldValue, byte newValue);

	public char readChar(Object obj, String name, char oldValue);

	public char writeChar(Object obj, String name, char oldValue, char newValue);

	public short readShort(Object obj, String name, short oldValue);

	public short writeShort(Object obj, String name, short oldValue, short newValue);

	public int readInt(Object obj, String name, int oldValue);

	public int writeInt(Object obj, String name, int oldValue, int newValue);

	public float readFloat(Object obj, String name, float oldValue);

	public float writeFloat(Object obj, String name, float oldValue, float newValue);

	public double readDouble(Object obj, String name, double oldValue);

	public double writeDouble(Object obj, String name, double oldValue, double newValue);

	public long readLong(Object obj, String name, long oldValue);

	public long writeLong(Object obj, String name, long oldValue, long newValue);

	public Object readObject(Object obj, String name, Object oldValue);

	public Object writeObject(Object obj, String name, Object oldValue, Object newValue);

}

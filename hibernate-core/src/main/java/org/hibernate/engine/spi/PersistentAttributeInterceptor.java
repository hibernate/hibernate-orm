/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

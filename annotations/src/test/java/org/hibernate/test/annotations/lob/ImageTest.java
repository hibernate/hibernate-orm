//$Id$
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.test.annotations.lob;

import junit.framework.AssertionFailedError;

import org.hibernate.Session;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.Sybase11Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.junit.RequiresDialect;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.util.ArrayHelper;

/**
 * Tests eager materialization and mutation of data mapped by
 * {@link org.hibernate.type.ImageType}.
 * 
 * @author Gail Badner
 */
@RequiresDialect( { SybaseASE15Dialect.class, SQLServerDialect.class,
		SybaseDialect.class, Sybase11Dialect.class })
public class ImageTest extends TestCase {
	private static final int ARRAY_SIZE = 10000;

	public void testBoundedLongByteArrayAccess() {
		byte[] original = buildRecursively(ARRAY_SIZE, true);
		byte[] changed = buildRecursively(ARRAY_SIZE, false);

		Session s = openSession();
		s.beginTransaction();
		ImageHolder entity = new ImageHolder();
		s.save(entity);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = (ImageHolder) s.get(ImageHolder.class, entity.getId());
		assertNull(entity.getLongByteArray());
		assertNull(entity.getDog());
		assertNull(entity.getPicByteArray());
		entity.setLongByteArray(original);
		Dog dog = new Dog();
		dog.setName("rabbit");
		entity.setDog(dog);
		entity.setPicByteArray(wrapPrimitive(original));
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = (ImageHolder) s.get(ImageHolder.class, entity.getId());
		assertEquals(ARRAY_SIZE, entity.getLongByteArray().length);
		assertEquals(original, entity.getLongByteArray());
		assertEquals(ARRAY_SIZE, entity.getPicByteArray().length);
		assertEquals(original, unwrapNonPrimitive(entity.getPicByteArray()));
		assertNotNull(entity.getDog());
		assertEquals(dog.getName(), entity.getDog().getName());
		entity.setLongByteArray(changed);
		entity.setPicByteArray(wrapPrimitive(changed));
		dog.setName("papa");
		entity.setDog(dog);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = (ImageHolder) s.get(ImageHolder.class, entity.getId());
		assertEquals(ARRAY_SIZE, entity.getLongByteArray().length);
		assertEquals(changed, entity.getLongByteArray());
		assertEquals(ARRAY_SIZE, entity.getPicByteArray().length);
		assertEquals(changed, unwrapNonPrimitive(entity.getPicByteArray()));
		assertNotNull(entity.getDog());
		assertEquals(dog.getName(), entity.getDog().getName());
		entity.setLongByteArray(null);
		entity.setPicByteArray(null);
		entity.setDog(null);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = (ImageHolder) s.get(ImageHolder.class, entity.getId());
		assertNull(entity.getLongByteArray());
		assertNull(entity.getDog());
		assertNull(entity.getPicByteArray());
		s.delete(entity);
		s.getTransaction().commit();
		s.close();
	}

	private Byte[] wrapPrimitive(byte[] bytes) {
		int length = bytes.length;
		Byte[] result = new Byte[length];
		for (int index = 0; index < length; index++) {
			result[index] = Byte.valueOf(bytes[index]);
		}
		return result;
	}

	private byte[] unwrapNonPrimitive(Byte[] bytes) {
		int length = bytes.length;
		byte[] result = new byte[length];
		for (int i = 0; i < length; i++) {
			result[i] = bytes[i].byteValue();
		}
		return result;
	}

	private byte[] buildRecursively(int size, boolean on) {
		byte[] data = new byte[size];
		data[0] = mask(on);
		for (int i = 0; i < size; i++) {
			data[i] = mask(on);
			on = !on;
		}
		return data;
	}

	private byte mask(boolean on) {
		return on ? (byte) 1 : (byte) 0;
	}

	public static void assertEquals(byte[] val1, byte[] val2) {
		if (!ArrayHelper.isEquals(val1, val2)) {
			throw new AssertionFailedError("byte arrays did not match");
		}
	}

	public ImageTest(String name) {
		super(name);
	}

	@Override
	protected String[] getAnnotatedPackages() {
		return new String[] { "org.hibernate.test.annotations.lob" };
	}

	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { ImageHolder.class };
	}

}
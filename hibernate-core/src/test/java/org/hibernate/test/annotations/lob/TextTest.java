/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.lob;

import java.util.Arrays;

import org.hibernate.Session;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.Sybase11Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.SybaseDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;
import junit.framework.AssertionFailedError;

import static org.junit.Assert.assertNull;

/**
 * Tests eager materialization and mutation of long strings.
 *
 * @author Steve Ebersole
 */
@RequiresDialect({SybaseASE15Dialect.class,SQLServerDialect.class,SybaseDialect.class,Sybase11Dialect.class})
public class TextTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { LongStringHolder.class };
	}

	private static final int LONG_STRING_SIZE = 10000;

	@Test
	public void testBoundedLongStringAccess() {
		String original = buildRecursively(LONG_STRING_SIZE, 'x');
		String changed = buildRecursively(LONG_STRING_SIZE, 'y');

		Session s = openSession();
		s.beginTransaction();
		LongStringHolder entity = new LongStringHolder();
		s.save(entity);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = (LongStringHolder) s.get(LongStringHolder.class, entity
				.getId());
		assertNull(entity.getLongString());
		assertNull(entity.getName());
		assertNull(entity.getWhatEver());
		entity.setLongString(original);
		entity.setName(original.toCharArray());
		entity.setWhatEver(wrapPrimitive(original.toCharArray()));
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = (LongStringHolder) s.get(LongStringHolder.class, entity
				.getId());
		Assert.assertEquals( LONG_STRING_SIZE, entity.getLongString().length() );
		Assert.assertEquals( original, entity.getLongString() );
		Assert.assertNotNull( entity.getName() );
		Assert.assertEquals( LONG_STRING_SIZE, entity.getName().length );
		assertEquals( original.toCharArray(), entity.getName() );
		Assert.assertNotNull( entity.getWhatEver() );
		Assert.assertEquals( LONG_STRING_SIZE, entity.getWhatEver().length );
		assertEquals( original.toCharArray(), unwrapNonPrimitive( entity.getWhatEver() ) );
		entity.setLongString(changed);
		entity.setName(changed.toCharArray());
		entity.setWhatEver(wrapPrimitive(changed.toCharArray()));
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = (LongStringHolder) s.get(LongStringHolder.class, entity
				.getId());
		Assert.assertEquals( LONG_STRING_SIZE, entity.getLongString().length() );
		Assert.assertEquals( changed, entity.getLongString() );
		Assert.assertNotNull( entity.getName() );
		Assert.assertEquals( LONG_STRING_SIZE, entity.getName().length );
		assertEquals( changed.toCharArray(), entity.getName() );
		Assert.assertNotNull( entity.getWhatEver() );
		Assert.assertEquals( LONG_STRING_SIZE, entity.getWhatEver().length );
		assertEquals( changed.toCharArray(), unwrapNonPrimitive( entity.getWhatEver() ) );
		entity.setLongString(null);
		entity.setName(null);
		entity.setWhatEver(null);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = (LongStringHolder) s.get(LongStringHolder.class, entity
				.getId());
		assertNull(entity.getLongString());
		assertNull(entity.getName());
		assertNull(entity.getWhatEver());
		s.delete(entity);
		s.getTransaction().commit();
		s.close();
	}

	public static void assertEquals(char[] val1, char[] val2) {
		if ( !Arrays.equals( val1, val2 ) ) {
			throw new AssertionFailedError("byte arrays did not match");
		}
	}

	private String buildRecursively(int size, char baseChar) {
		StringBuilder buff = new StringBuilder();
		for (int i = 0; i < size; i++) {
			buff.append(baseChar);
		}
		return buff.toString();
	}

	private Character[] wrapPrimitive(char[] bytes) {
		int length = bytes.length;
		Character[] result = new Character[length];
		for (int index = 0; index < length; index++) {
			result[index] = Character.valueOf(bytes[index]);
		}
		return result;
	}

	private char[] unwrapNonPrimitive(Character[] bytes) {
		int length = bytes.length;
		char[] result = new char[length];
		for (int i = 0; i < length; i++) {
			result[i] = bytes[i].charValue();
		}
		return result;
	}

	@Override
	protected String[] getAnnotatedPackages() {
		return new String[] { "org.hibernate.test.annotations.lob" };
	}

}

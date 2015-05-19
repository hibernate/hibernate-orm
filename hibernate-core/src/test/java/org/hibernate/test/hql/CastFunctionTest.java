/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class CastFunctionTest extends BaseCoreFunctionalTestCase {
	@Entity( name="MyEntity" )
	public static class MyEntity {
		@Id
		private Integer id;
		private String name;
		private Double theLostNumber;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class };
	}

	@Test
	public void testStringCasting() {
		Session s = openSession();
		s.beginTransaction();

		// using the short name
		s.createQuery( "select cast(e.theLostNumber as string) from MyEntity e" ).list();
		// using the java class name
		s.createQuery( "select cast(e.theLostNumber as java.lang.String) from MyEntity e" ).list();
		// using the fqn Hibernate Type name
		s.createQuery( "select cast(e.theLostNumber as org.hibernate.type.StringType) from MyEntity e" ).list();

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testIntegerCasting() {
		Session s = openSession();
		s.beginTransaction();

		// using the short name
		s.createQuery( "select cast(e.theLostNumber as integer) from MyEntity e" ).list();
		// using the java class name (primitive)
		s.createQuery( "select cast(e.theLostNumber as int) from MyEntity e" ).list();
		// using the java class name
		s.createQuery( "select cast(e.theLostNumber as java.lang.Integer) from MyEntity e" ).list();
		// using the fqn Hibernate Type name
		s.createQuery( "select cast(e.theLostNumber as org.hibernate.type.IntegerType) from MyEntity e" ).list();

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testLongCasting() {
		Session s = openSession();
		s.beginTransaction();

		// using the short name (also the primitive name)
		s.createQuery( "select cast(e.theLostNumber as long) from MyEntity e" ).list();
		// using the java class name
		s.createQuery( "select cast(e.theLostNumber as java.lang.Long) from MyEntity e" ).list();
		// using the fqn Hibernate Type name
		s.createQuery( "select cast(e.theLostNumber as org.hibernate.type.LongType) from MyEntity e" ).list();

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testFloatCasting() {
		Session s = openSession();
		s.beginTransaction();

		// using the short name (also the primitive name)
		s.createQuery( "select cast(e.theLostNumber as float) from MyEntity e" ).list();
		// using the java class name
		s.createQuery( "select cast(e.theLostNumber as java.lang.Float) from MyEntity e" ).list();
		// using the fqn Hibernate Type name
		s.createQuery( "select cast(e.theLostNumber as org.hibernate.type.FloatType) from MyEntity e" ).list();

		s.getTransaction().commit();
		s.close();
	}
}

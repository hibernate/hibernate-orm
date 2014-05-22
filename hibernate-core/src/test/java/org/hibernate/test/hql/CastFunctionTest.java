/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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

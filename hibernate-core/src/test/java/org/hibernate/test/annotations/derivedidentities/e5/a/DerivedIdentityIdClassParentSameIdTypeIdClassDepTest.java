/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.derivedidentities.e5.a;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.test.util.SchemaUtil;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
@FailureExpectedWithNewMetamodel
public class DerivedIdentityIdClassParentSameIdTypeIdClassDepTest extends BaseCoreFunctionalTestCase {
	private static final String FIRST_NAME = "Emmanuel";
	private static final String LAST_NAME = "Bernard";

	@Test
	public void testOneToOneExplicitJoinColumn() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK1", metadata() ) );
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK2", metadata() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "MedicalHistory", "firstname", metadata() ) );

		Session s = openSession();
		s.getTransaction().begin();
		Person e = new Person( FIRST_NAME, LAST_NAME );
		s.persist( e );
		MedicalHistory d = new MedicalHistory( e );
		s.persist( d );
		s.flush();
		s.refresh( d );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		PersonId pId = new PersonId( FIRST_NAME, LAST_NAME );
		MedicalHistory d2 = (MedicalHistory) s.get( MedicalHistory.class, pId );
		Person p2 = (Person) s.get( Person.class, pId );
		assertEquals( pId.firstName, d2.patient.firstName );
		assertEquals( pId.firstName, p2.firstName );
		s.delete( d2 );
		s.delete( p2 );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testTckLikeBehavior() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK1", metadata() ) );
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK2", metadata() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "MedicalHistory", "firstname", metadata() ) );

		Session s = openSession();
		s.getTransaction().begin();
		Person e = new Person( FIRST_NAME, LAST_NAME );
		s.persist( e );
		MedicalHistory d = new MedicalHistory( e );
		s.persist( d );
		s.flush();
		s.refresh( d );
		s.getTransaction().commit();

		// NOTE THAT WE LEAVE THE SESSION OPEN!

		s.getTransaction().begin();
		PersonId pId = new PersonId( FIRST_NAME, LAST_NAME );
		MedicalHistory d2 = (MedicalHistory) s.get( MedicalHistory.class, pId );
		Person p2 = (Person) s.get( Person.class, pId );
		assertEquals( pId.firstName, d2.patient.firstName );
		assertEquals( pId.firstName, p2.firstName );
		s.delete( d2 );
		s.delete( p2 );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				MedicalHistory.class,
				Person.class
		};
	}
}

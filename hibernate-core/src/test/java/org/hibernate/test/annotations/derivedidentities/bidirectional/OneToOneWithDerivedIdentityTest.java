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
package org.hibernate.test.annotations.derivedidentities.bidirectional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class OneToOneWithDerivedIdentityTest extends BaseCoreFunctionalTestCase {
	@Test
	@TestForIssue(jiraKey = "HHH-5695")
	@FailureExpectedWithNewMetamodel
	public void testInsertFooAndBarWithDerivedId() {
		Session s = openSession();
		s.beginTransaction();
		Bar bar = new Bar();
		bar.setDetails( "Some details" );
		Foo foo = new Foo();
		foo.setBar( bar );
		bar.setFoo( foo );
		s.persist( foo );
		s.flush();
		assertNotNull( foo.getId() );
		assertEquals( foo.getId(), bar.getFoo().getId() );

		s.clear();
		Bar newBar = ( Bar ) s.createQuery( "SELECT b FROM Bar b WHERE b.foo.id = :id" )
				.setParameter( "id", foo.getId() )
				.uniqueResult();
		assertNotNull( newBar );
		assertEquals( "Some details", newBar.getDetails() );
		s.getTransaction().rollback();
		s.close();
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-6813")
	public void testSelectWithDerivedId() {
		Session s = openSession();
		s.beginTransaction();
		Bar bar = new Bar();
		bar.setDetails( "Some details" );
		Foo foo = new Foo();
		foo.setBar( bar );
		bar.setFoo( foo );
		s.persist( foo );
		s.flush();
		assertNotNull( foo.getId() );
		assertEquals( foo.getId(), bar.getFoo().getId() );

		s.clear();
		Foo newFoo = (Foo) s.createQuery( "SELECT f FROM Foo f" ).uniqueResult();
		assertNotNull( newFoo );
		assertEquals( "Some details", newFoo.getBar().getDetails() );
		s.getTransaction().rollback();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6813")
	@FailureExpectedWithNewMetamodel
	// Regression test utilizing multiple types of queries.
	public void testCase() {
		Session s = openSession();
		s.getTransaction().begin();

		Person p = new Person();
		p.setName( "Alfio" );
		PersonInfo pi = new PersonInfo();
		pi.setId( p );
		pi.setInfo( "Some information" );
		s.persist( p );
		s.persist( pi );

		s.getTransaction().commit();
		s.clear();

		s.getTransaction().begin();

		Query q = s.getNamedQuery( "PersonQuery" );
		List<Person> persons = q.list();
		assertEquals( persons.size(), 1 );
		assertEquals( persons.get( 0 ).getName(), "Alfio" );

		s.getTransaction().commit();
		s.clear();

		s.getTransaction().begin();

		p = (Person) s.get( Person.class, persons.get( 0 ).getId() );
		assertEquals( p.getName(), "Alfio" );

		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Foo.class,
				Bar.class,
				Person.class,
				PersonInfo.class
		};
	}

}

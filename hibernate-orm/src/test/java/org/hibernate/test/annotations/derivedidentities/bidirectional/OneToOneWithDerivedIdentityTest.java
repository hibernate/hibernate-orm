/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.bidirectional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class OneToOneWithDerivedIdentityTest extends BaseCoreFunctionalTestCase {
	@Test
	@TestForIssue(jiraKey = "HHH-5695")
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
	@TestForIssue( jiraKey = "HHH-10476")
	public void testInsertFooAndBarWithDerivedIdPC() {
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
		Bar barWithFoo = new Bar();
		barWithFoo.setFoo( foo );
		barWithFoo.setDetails( "wrong details" );
		bar = (Bar) s.get( Bar.class, barWithFoo );
		assertSame( bar, barWithFoo );
		assertEquals( "Some details", bar.getDetails() );
		SessionImplementor si = (SessionImplementor) s;
		assertTrue( si.getPersistenceContext().isEntryFor( bar ) );
		assertFalse( si.getPersistenceContext().isEntryFor( bar.getFoo() ) );

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

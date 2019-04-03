/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.fetch;

import java.util.Date;

import org.hibernate.Hibernate;
import org.hibernate.Transaction;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class FetchingTest extends SessionFactoryBasedFunctionalTest {

	@Test
	public void testLazy() {
		inSession(
				session -> {
					try {
						Transaction tx = session.beginTransaction();

						Person p = new Person( "Gavin", "King", "JBoss Inc" );
						Stay stay = new Stay( p, new Date(), new Date(), "A380", "Blah", "Blah" );
						p.addStay( stay );
						session.persist( p );
						tx.commit();
						session.clear();
						tx = session.beginTransaction();
						p = (Person) session.createQuery( "from Person p where p.firstName = :name" )
								.setParameter( "name", "Gavin" ).uniqueResult();
						assertFalse( Hibernate.isInitialized( p.getStays() ) );
						session.delete( p );
						tx.commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Test
	@FailureExpected("Extra lazy not yet implemented")
	public void testExtraLazy() {
		inSession(
				session -> {
					try {
						Transaction tx = session.beginTransaction();
						Person p = new Person( "Gavin", "King", "JBoss Inc" );
						Stay stay = new Stay( p, new Date(), new Date(), "A380", "Blah", "Blah" );
						p.getOrderedStay().add( stay );
						session.persist( p );
						tx.commit();
						session.clear();
						tx = session.beginTransaction();
						p = (Person) session.createQuery( "from Person p where p.firstName = :name" )
								.setParameter( "name", "Gavin" ).uniqueResult();
						assertFalse( Hibernate.isInitialized( p.getOrderedStay() ) );
						assertEquals( 1, p.getOrderedStay().size() );
						assertFalse( Hibernate.isInitialized( p.getOrderedStay() ) );
						assertEquals( "A380", p.getOrderedStay().get( 0 ).getVessel() );
						assertFalse( Hibernate.isInitialized( p.getOrderedStay() ) );
						session.delete( p );
						tx.commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
				} );
	}

	@Test
	public void testHibernateFetchingLazy() {
		inSession(
				session -> {
					try {
						Transaction tx = session.beginTransaction();
						Person p = new Person( "Gavin", "King", "JBoss Inc" );
						Stay stay = new Stay( null, new Date(), new Date(), "A380", "Blah", "Blah" );
						Stay stay2 = new Stay( null, new Date(), new Date(), "A320", "Blah", "Blah" );
						Stay stay3 = new Stay( null, new Date(), new Date(), "A340", "Blah", "Blah" );
						stay.setOldPerson( p );
						stay2.setVeryOldPerson( p );
						stay3.setVeryOldPerson( p );
						p.addOldStay( stay );
						p.addVeryOldStay( stay2 );
						p.addVeryOldStay( stay3 );
						session.persist( p );
						tx.commit();
						session.clear();
						tx = session.beginTransaction();
						p = (Person) session.createQuery( "from Person p where p.firstName = :name" )
								.setParameter( "name", "Gavin" ).uniqueResult();
						assertFalse( Hibernate.isInitialized( p.getOldStays() ) );
						assertEquals( 1, p.getOldStays().size() );
						assertFalse( Hibernate.isInitialized( p.getOldStays() ), "lazy extra is failing" );
						session.clear();
						stay = session.get( Stay.class, stay.getId() );
						assertTrue( !Hibernate.isInitialized( stay.getOldPerson() ) );
						session.clear();
						stay3 = session.get( Stay.class, stay3.getId() );
						assertTrue(
								Hibernate.isInitialized( stay3.getVeryOldPerson() ),
								"FetchMode.JOIN should overrides lazy options"
						);
						session.delete( stay3.getVeryOldPerson() );
						tx.commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}

				}
		);
	}

	@Test
	public void testOneToManyFetchEager() {
		inTransaction(
				session -> {
					Branch b = new Branch();
					session.persist( b );
					session.flush();
					Leaf l = new Leaf();
					l.setBranch( b );
					session.persist( l );
					session.flush();

					session.clear();

					session.createQuery( "from Branch b" ).list();
				}
		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				Stay.class,
				Branch.class,
				Leaf.class
		};
	}
}

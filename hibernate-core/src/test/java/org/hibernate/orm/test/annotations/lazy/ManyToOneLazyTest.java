/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.lazy;

import java.util.UUID;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.junit.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ManyToOneLazyTest extends BaseCoreFunctionalTestCase {

	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Company.class,
				Person.class,
				Ticket.class,
				Bug.class,
				Reference.class,
				Update.class,
				ReplicatedUpdate.class
		};
	}

	@Override
	protected void prepareTest() throws Exception {
		try (Session writeSession = openSession()) {
			final Transaction writeTransaction = writeSession.beginTransaction();
			Company company = new Company();
			company.setName( "The Company" );
			writeSession.persist( company );

			Person alice = new Person();
			alice.setName( "Alice" );
			alice.setGuid( UUID.randomUUID().toString() );
			writeSession.persist( alice );

			Person bob = new Person();
			bob.setName( "Bob" );
			bob.setGuid( UUID.randomUUID().toString() );
			writeSession.persist( bob );

			Bug bug = new Bug();
			bug.setGuid( UUID.randomUUID().toString() );
			bug.setDescription( "Coffee machine unplugged" );
			writeSession.persist( bug );

			Reference reference = new Reference();
			reference.setUrlCode( "TEST-1234" );

			writeSession.persist( reference );

			Ticket ticket = new Ticket();
			ticket.setTitle( "Coffee machine broken" );
			ticket.setRequester( bob );
			ticket.setAssignee( alice );
			ticket.setCompany(company);
			ticket.setReference( reference );
			ticket.setBug( bug );
			ticket.setGuid( UUID.randomUUID().toString() );

			writeSession.persist( ticket );

			Update update = new Update();
			update.setBody( "Plugged coffee machine back in" );
			update.setTicket( ticket );

			writeSession.persist( update );

			ReplicatedUpdate replicatedUpdate = new ReplicatedUpdate();
			replicatedUpdate.setBody( update.getBody() );
			replicatedUpdate.setTicket( ticket );

			writeSession.persist( replicatedUpdate );

			writeTransaction.commit();
		}
	}

	@Test
	public void testManyToOneLazyLoadedEntitiesAreOnlyInitializedAfterReference() {
		try (Session readSession = openSession()) {
			Ticket ticket = getTicket(readSession);

			// Joined by Id
			assertFalse( Hibernate.isInitialized( ticket.getCompany() ) );
			assertEquals( "The Company", ticket.getCompany().getName() );
			assertTrue( Hibernate.isInitialized( ticket.getCompany() ) );

			// Joined by NaturalId
			assertFalse( Hibernate.isInitialized( ticket.getAssignee() ) );
			assertEquals( "Alice", ticket.getAssignee().getName() );
			assertTrue( Hibernate.isInitialized( ticket.getAssignee() ) );

			// Also Joined by NaturalId
			assertFalse( Hibernate.isInitialized( ticket.getRequester() ) );
			assertEquals( "Bob", ticket.getRequester().getName() );
			assertTrue( Hibernate.isInitialized( ticket.getRequester() ) );
		}
	}

	@Test
	public void testOneToOneLazyLoadedEntitiesAreOnlyInitializedAfterReference() {
		try (Session readSession = openSession()) {
			Ticket ticket = getTicket( readSession );

			// Joined by Id
			assertFalse( Hibernate.isInitialized( ticket.getReference() ) );
			assertEquals( "TEST-1234", ticket.getReference().getUrlCode() );
			assertTrue( Hibernate.isInitialized( ticket.getReference() ) );

			// Joined by NaturalId
			assertFalse( Hibernate.isInitialized( ticket.getBug() ) );
			assertEquals( "Coffee machine unplugged", ticket.getBug().getDescription() );
			assertTrue( Hibernate.isInitialized( ticket.getBug() ) );
		}
	}

	@Test
	public void testOneToManyLazyLoadedEntitiesAreOnlyInitializedAfterReference() {
		try (Session readSession = openSession()) {
			Ticket ticket = getTicket( readSession );

			// Joined by Id
			assertFalse( Hibernate.isInitialized( ticket.getUpdates() ) );
			assertEquals( 1, ticket.getUpdates().size() ); // Size check counts as reference
			assertTrue( Hibernate.isInitialized( ticket.getUpdates() ) );

			// Joined by Guid
			assertFalse( Hibernate.isInitialized( ticket.getReplicatedUpdates() ) );
			assertEquals( 1, ticket.getReplicatedUpdates().size() ); // Size check counts as reference
			assertTrue( Hibernate.isInitialized( ticket.getReplicatedUpdates() ) );
		}
	}

	private Ticket getTicket(Session readSession) {
		final CriteriaBuilder criteriaBuilder = readSession.getSessionFactory().getCriteriaBuilder();
		final CriteriaQuery<Ticket> criteria = criteriaBuilder.createQuery( Ticket.class );
		final Root<Ticket> root = criteria.from( Ticket.class );
		criteria.where( criteriaBuilder.equal( root.get( "title" ), "Coffee machine broken" ) );

		return session.createQuery(criteria).uniqueResult();
	}
}

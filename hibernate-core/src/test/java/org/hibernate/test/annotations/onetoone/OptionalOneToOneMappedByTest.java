/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetoone;

import javax.persistence.PersistenceException;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 * @author Gail Badner
 */
public class OptionalOneToOneMappedByTest extends BaseCoreFunctionalTestCase {

	// @OneToOne(mappedBy="address") with foreign generator
	@Test
	public void testBidirForeignIdGenerator() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		OwnerAddress address = new OwnerAddress();
		address.setOwner( null );
		try {
			s.persist( address );
			s.flush();
			fail( "should have failed with IdentifierGenerationException" );
		}
		catch (PersistenceException ex) {
			assertTyping(IdentifierGenerationException.class, ex.getCause());
			// expected
		}
		finally {
			tx.rollback();
		}
		s.close();
	}

	@Test
	public void testBidirAssignedId() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		PartyAffiliate affiliate = new PartyAffiliate();
		affiliate.partyId = "id";

		s.persist( affiliate );
		s.getTransaction().commit();

		s.clear();

		Transaction tx = s.beginTransaction();

		affiliate = ( PartyAffiliate ) s.createCriteria(PartyAffiliate.class)
				.add( Restrictions.idEq( "id" ) )
				.uniqueResult();
		assertNotNull( affiliate );
		assertEquals( "id", affiliate.partyId );
		assertNull( affiliate.party );

		s.clear();

		affiliate = ( PartyAffiliate ) s.get( PartyAffiliate.class, "id" );
		assertNull( affiliate.party );

		s.delete( affiliate );
		tx.commit();
		s.close();
	}

	@Test
	public void testBidirDefaultIdGenerator() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		PersonAddress personAddress = new PersonAddress();
		personAddress.setPerson( null );

		s.persist( personAddress );
		s.getTransaction().commit();

		s.clear();

		Transaction tx = s.beginTransaction();

		personAddress = ( PersonAddress ) s.createCriteria(PersonAddress.class)
				.add( Restrictions.idEq( personAddress.getId() ) )
				.uniqueResult();
		assertNotNull( personAddress );
		assertNull( personAddress.getPerson() );

		s.clear();

		personAddress = ( PersonAddress ) s.get( PersonAddress.class, personAddress.getId() );
		assertNull( personAddress.getPerson() );

		s.delete( personAddress );
		tx.commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Party.class,
				PartyAffiliate.class,
				Owner.class,
				OwnerAddress.class,
				Person.class,
				PersonAddress.class
		};
	}
}

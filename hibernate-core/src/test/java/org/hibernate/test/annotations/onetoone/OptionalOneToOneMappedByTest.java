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
package org.hibernate.test.annotations.onetoone;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

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
	@FailureExpectedWithNewMetamodel( message = "mappedBy @OneToOne with foreign generator" )
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
		catch (IdentifierGenerationException ex) {
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

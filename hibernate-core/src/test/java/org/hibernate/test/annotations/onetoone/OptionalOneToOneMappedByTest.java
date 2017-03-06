/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetoone;

import javax.persistence.PersistenceException;

import org.hibernate.criterion.Restrictions;
import org.hibernate.id.IdentifierGenerationException;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
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
		try {
			doInHibernate( this::sessionFactory, session -> {
				OwnerAddress address = new OwnerAddress();
				address.setOwner( null );

				session.persist( address );
				session.flush();
				fail( "should have failed with IdentifierGenerationException" );
			} );
		}
		catch (PersistenceException ex) {
			assertTyping( IdentifierGenerationException.class, ex.getCause() );
			// expected
		}
	}

	@Test
	public void testBidirAssignedId() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			PartyAffiliate affiliate = new PartyAffiliate();
			affiliate.partyId = "id";

			session.persist( affiliate );
		} );

		doInHibernate( this::sessionFactory, session -> {
			PartyAffiliate affiliate = (PartyAffiliate) session.createCriteria(
					PartyAffiliate.class )
					.add( Restrictions.idEq( "id" ) )
					.uniqueResult();
			assertNotNull( affiliate );
			assertEquals( "id", affiliate.partyId );
			assertNull( affiliate.party );
		} );

		doInHibernate( this::sessionFactory, session -> {
			PartyAffiliate affiliate = session.get(
					PartyAffiliate.class,
					"id"
			);
			assertNull( affiliate.party );

			session.delete( affiliate );
		} );
	}

	@Test
	public void testBidirDefaultIdGenerator() throws Exception {
		PersonAddress _personAddress = doInHibernate(
				this::sessionFactory,
				session -> {
					PersonAddress personAddress = new PersonAddress();
					personAddress.setPerson( null );

					session.persist( personAddress );

					return personAddress;
				}
		);

		doInHibernate( this::sessionFactory, session -> {
			PersonAddress personAddress = (PersonAddress) session.createCriteria(
					PersonAddress.class )
					.add( Restrictions.idEq( _personAddress.getId() ) )
					.uniqueResult();
			assertNotNull( personAddress );
			assertNull( personAddress.getPerson() );
		} );

		doInHibernate( this::sessionFactory, session -> {
			PersonAddress personAddress = session.get(
					PersonAddress.class,
					_personAddress.getId()
			);
			assertNull( personAddress.getPerson() );

			session.delete( personAddress );
		} );
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

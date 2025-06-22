/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.id.IdentifierGenerationException;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 * @author Gail Badner
 */
public class OptionalOneToOnePKJCTest extends BaseCoreFunctionalTestCase {

	@Test
	@JiraKey( value = "HHH-4982")
	public void testNullBidirForeignIdGenerator() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Person person = new Person();
		person.setPersonAddress( null );
		try {
			s.persist( person );
			s.flush();
			fail( "should have thrown IdentifierGenerationException.");
		}
		catch (PersistenceException ex) {
			assertTyping(IdentifierGenerationException.class, ex);
			// expected
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	@JiraKey( value = "HHH-4982")
	public void testNotFoundBidirForeignIdGenerator() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Person person = new Person();
		person.setPersonAddress( null );
		person.setId( 1 );
		try {
			// Hibernate resets the ID to null before executing the foreign generator
			s.persist( person );
			s.flush();
			fail( "should have thrown IdentifierGenerationException.");
		}
		catch (PersistenceException ex) {
			assertTyping(IdentifierGenerationException.class, ex);
			// expected
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	// @PrimaryKeyJoinColumn @OneToOne(optional=true) non-foreign generator
	@Test
	@JiraKey( value = "HHH-4982")
	public void testNotFoundBidirDefaultIdGenerator() {
		Session s = openSession();
		s.getTransaction().begin();
		Owner owner = new Owner();
		owner.setAddress( null );
		s.persist( owner );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		owner = ( Owner ) s.get( Owner.class, owner.getId() );
		assertNotNull( owner );
		assertNull( owner.getAddress() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Owner> criteria = criteriaBuilder.createQuery( Owner.class );
		Root<Owner> root = criteria.from( Owner.class );
		criteria.where( criteriaBuilder.equal( root.get( "id" ), owner.getId() ) );

		owner = s.createQuery( criteria ).uniqueResult();
//		owner = ( Owner ) s.createCriteria( Owner.class )
//				.add( Restrictions.idEq( owner.getId() ) )
//				.uniqueResult();
		assertNotNull( owner );
		assertNull( owner.getAddress() );
		s.remove( owner );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testNotFoundBidirAssignedId() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		Party party = new Party();
		party.partyId = "id";
		party.partyAffiliate = null;
		s.persist( party );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		party = ( Party ) s.get( Party.class, "id" );
		assertNull( party.partyAffiliate );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();

		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<Party> criteria = criteriaBuilder.createQuery( Party.class );
		Root<Party> root = criteria.from( Party.class );
		criteria.where( criteriaBuilder.equal( root.get( "partyId" ), "id" ) );

		party = s.createQuery( criteria ).uniqueResult();
//		party = ( Party ) s.createCriteria( Party.class )
//				.add( Restrictions.idEq( "id" ) )
//				.uniqueResult();
		assertNotNull( party );
		assertEquals( "id", party.partyId );
		assertNull( party.partyAffiliate );
		s.remove( party );
		s.getTransaction().commit();
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

	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] { "org/hibernate/orm/test/annotations/onetoone/orm.xml" };
	}
}

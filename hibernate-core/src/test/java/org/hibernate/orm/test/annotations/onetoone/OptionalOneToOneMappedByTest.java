/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone;

import java.util.concurrent.atomic.AtomicReference;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.id.IdentifierGenerationException;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
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
			assertTyping( IdentifierGenerationException.class, ex );
			// expected
		}
	}

	@Test
	public void testBidirAssignedId() {
		doInHibernate( this::sessionFactory, session -> {
			PartyAffiliate affiliate = new PartyAffiliate();
			affiliate.partyId = "id";

			session.persist( affiliate );
		} );

		doInHibernate( this::sessionFactory, session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<PartyAffiliate> criteria = criteriaBuilder.createQuery( PartyAffiliate.class );
			Root<PartyAffiliate> root = criteria.from( PartyAffiliate.class );
			criteria.where( criteriaBuilder.equal( root.get("partyId"), "id" ) );

			PartyAffiliate affiliate = session.createQuery( criteria ).uniqueResult();
//			PartyAffiliate affiliate = (PartyAffiliate) session.createCriteria(
//					PartyAffiliate.class )
//					.add( Restrictions.idEq( "id" ) )
//					.uniqueResult();
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

			session.remove( affiliate );
		} );
	}

	@Test
	public void testBidirDefaultIdGenerator() {
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
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<PersonAddress> criteria = criteriaBuilder.createQuery( PersonAddress.class );
			Root<PersonAddress> root = criteria.from( PersonAddress.class );
			criteria.where( criteriaBuilder.equal( root.get("id"), _personAddress.getId()) );
			PersonAddress personAddress = session.createQuery( criteria ).uniqueResult();
//			PersonAddress personAddress = (PersonAddress) session.createCriteria(
//					PersonAddress.class )
//					.add( Restrictions.idEq( _personAddress.getId() ) )
//					.uniqueResult();
			assertNotNull( personAddress );
			assertNull( personAddress.getPerson() );
		} );

		doInHibernate( this::sessionFactory, session -> {
			PersonAddress personAddress = session.get(
					PersonAddress.class,
					_personAddress.getId()
			);
			assertNull( personAddress.getPerson() );

			session.remove( personAddress );
		} );
	}

	@Test
	@JiraKey(value = "HHH-5757")
	public void testBidirQueryEntityProperty() {

		AtomicReference<Person> personHolder = new AtomicReference<>();

		PersonAddress _personAddress = doInHibernate(
				this::sessionFactory,
				session -> {
					PersonAddress personAddress = new PersonAddress();
					Person person = new Person();
					personAddress.setPerson( person );
					person.setPersonAddress( personAddress );

					session.persist( person );
					session.persist( personAddress );

					personHolder.set( person );

					return personAddress;
				}
		);

		doInHibernate( this::sessionFactory, session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<PersonAddress> criteria = criteriaBuilder.createQuery( PersonAddress.class );
			Root<PersonAddress> root = criteria.from( PersonAddress.class );
			criteria.where( criteriaBuilder.equal( root.get("id"), _personAddress.getId()) );
			PersonAddress personAddress = session.createQuery( criteria ).uniqueResult();
//			PersonAddress personAddress = (PersonAddress) session.createCriteria(
//					PersonAddress.class )
//					.add( Restrictions.idEq( _personAddress.getId() ) )
//					.uniqueResult();
			assertNotNull( personAddress );
			assertNotNull( personAddress.getPerson() );
		} );

		doInHibernate( this::sessionFactory, session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			Person person = personHolder.get();
			// this call throws GenericJDBCException
			PersonAddress personAddress = session.createQuery(
					"select pa from PersonAddress pa where pa.person = :person", PersonAddress.class )
					.setParameter( "person", person )
					.getSingleResult();

			CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
			Root<Person> root = criteria.from( Person.class );
			criteria.where( criteriaBuilder.equal( root.get("personAddress"), personAddress ) );

			session.createQuery( criteria ).uniqueResult();
			// the other way should also work
//			person = (Person) session.createCriteria( Person.class )
//					.add( Restrictions.eq( "personAddress", personAddress ) )
//					.uniqueResult();

			session.remove( personAddress );
			assertNotSame( person, personAddress.getPerson() );
			personAddress.getPerson().setPersonAddress( null );
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

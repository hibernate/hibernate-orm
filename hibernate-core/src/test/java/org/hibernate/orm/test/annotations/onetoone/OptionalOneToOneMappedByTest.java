/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Emmanuel Bernard
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				Party.class,
				PartyAffiliate.class,
				Owner.class,
				OwnerAddress.class,
				Person.class,
				PersonAddress.class
		}
)
@SessionFactory
public class OptionalOneToOneMappedByTest {

	// @OneToOne(mappedBy="address") with foreign generator
	@Test
	public void testBidirForeignIdGenerator(SessionFactoryScope scope) {
		assertThrows( IdentifierGenerationException.class, () -> scope.inTransaction( session -> {
			OwnerAddress address = new OwnerAddress();
			address.setOwner( null );

			session.persist( address );
			session.flush();
			fail( "should have failed with IdentifierGenerationException" );
		} ) );
	}

	@Test
	public void testBidirAssignedId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			PartyAffiliate affiliate = new PartyAffiliate();
			affiliate.partyId = "id";

			session.persist( affiliate );
		} );

		scope.inTransaction( session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<PartyAffiliate> criteria = criteriaBuilder.createQuery( PartyAffiliate.class );
			Root<PartyAffiliate> root = criteria.from( PartyAffiliate.class );
			criteria.where( criteriaBuilder.equal( root.get( "partyId" ), "id" ) );

			PartyAffiliate affiliate = session.createQuery( criteria ).uniqueResult();
//			PartyAffiliate affiliate = (PartyAffiliate) session.createCriteria(
//					PartyAffiliate.class )
//					.add( Restrictions.idEq( "id" ) )
//					.uniqueResult();
			assertThat( affiliate ).isNotNull();
			assertThat( affiliate.partyId ).isEqualTo( "id" );
			assertThat( affiliate.party ).isNull();
		} );

		scope.inTransaction( session -> {
			PartyAffiliate affiliate = session.find(
					PartyAffiliate.class,
					"id"
			);
			assertThat( affiliate.party ).isNull();

			session.remove( affiliate );
		} );
	}

	@Test
	public void testBidirDefaultIdGenerator(SessionFactoryScope scope) {
		PersonAddress _personAddress = scope.fromTransaction(
				session -> {
					PersonAddress personAddress = new PersonAddress();
					personAddress.setPerson( null );

					session.persist( personAddress );

					return personAddress;
				}
		);

		scope.inTransaction( session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<PersonAddress> criteria = criteriaBuilder.createQuery( PersonAddress.class );
			Root<PersonAddress> root = criteria.from( PersonAddress.class );
			criteria.where( criteriaBuilder.equal( root.get( "id" ), _personAddress.getId() ) );
			PersonAddress personAddress = session.createQuery( criteria ).uniqueResult();
//			PersonAddress personAddress = (PersonAddress) session.createCriteria(
//					PersonAddress.class )
//					.add( Restrictions.idEq( _personAddress.getId() ) )
//					.uniqueResult();
			assertThat( personAddress ).isNotNull();
			assertThat( personAddress.getPerson() ).isNull();
		} );

		scope.inTransaction( session -> {
			PersonAddress personAddress = session.find(
					PersonAddress.class,
					_personAddress.getId()
			);
			assertThat( personAddress.getPerson() ).isNull();

			session.remove( personAddress );
		} );
	}

	@Test
	@JiraKey(value = "HHH-5757")
	public void testBidirQueryEntityProperty(SessionFactoryScope scope) {

		AtomicReference<Person> personHolder = new AtomicReference<>();

		PersonAddress _personAddress = scope.fromTransaction( session -> {
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

		scope.inTransaction( session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<PersonAddress> criteria = criteriaBuilder.createQuery( PersonAddress.class );
			Root<PersonAddress> root = criteria.from( PersonAddress.class );
			criteria.where( criteriaBuilder.equal( root.get( "id" ), _personAddress.getId() ) );
			PersonAddress personAddress = session.createQuery( criteria ).uniqueResult();
//			PersonAddress personAddress = (PersonAddress) session.createCriteria(
//					PersonAddress.class )
//					.add( Restrictions.idEq( _personAddress.getId() ) )
//					.uniqueResult();
			assertThat( personAddress ).isNotNull();
			assertThat( personAddress.getPerson() ).isNotNull();
		} );

		scope.inTransaction( session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			Person person = personHolder.get();
			// this call throws GenericJDBCException
			PersonAddress personAddress = session.createQuery(
							"select pa from PersonAddress pa where pa.person = :person", PersonAddress.class )
					.setParameter( "person", person )
					.getSingleResult();

			CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
			Root<Person> root = criteria.from( Person.class );
			criteria.where( criteriaBuilder.equal( root.get( "personAddress" ), personAddress ) );

			session.createQuery( criteria ).uniqueResult();
			// the other way should also work
//			person = (Person) session.createCriteria( Person.class )
//					.add( Restrictions.eq( "personAddress", personAddress ) )
//					.uniqueResult();

			session.remove( personAddress );
			assertThat( personAddress.getPerson() ).isNotSameAs( person );
			personAddress.getPerson().setPersonAddress( null );
		} );
	}
}

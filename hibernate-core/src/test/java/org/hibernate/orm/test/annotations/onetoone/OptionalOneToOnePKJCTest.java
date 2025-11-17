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
		},
		xmlMappings = "org/hibernate/orm/test/annotations/onetoone/orm.xml"
)
@SessionFactory
public class OptionalOneToOnePKJCTest {

	@Test
	@JiraKey(value = "HHH-4982")
	public void testNullBidirForeignIdGenerator(SessionFactoryScope scope) {
		assertThrows( IdentifierGenerationException.class, () -> scope.inTransaction(
				session -> {
					Person person = new Person();
					person.setPersonAddress( null );
					session.persist( person );
					session.flush();
					fail( "should have thrown IdentifierGenerationException." );
				}
		) );
	}

	@Test
	@JiraKey(value = "HHH-4982")
	public void testNotFoundBidirForeignIdGenerator(SessionFactoryScope scope) {
		assertThrows( IdentifierGenerationException.class, () -> scope.inTransaction(
				session -> {
					Person person = new Person();
					person.setPersonAddress( null );
					person.setId( 1 );
					// Hibernate resets the ID to null before executing the foreign generator
					session.persist( person );
					session.flush();
					fail( "should have thrown IdentifierGenerationException." );
				}
		) );
	}

	// @PrimaryKeyJoinColumn @OneToOne(optional=true) non-foreign generator
	@Test
	@JiraKey(value = "HHH-4982")
	public void testNotFoundBidirDefaultIdGenerator(SessionFactoryScope scope) {
		Owner o = new Owner();
		scope.inTransaction(
				session -> {
					o.setAddress( null );
					session.persist( o );
				}
		);

		scope.inTransaction(
				session -> {
					Owner owner = session.find( Owner.class, o.getId() );
					assertThat( owner ).isNotNull();
					assertThat( owner.getAddress() ).isNull();
				}
		);

		scope.inTransaction(
				session -> {
					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<Owner> criteria = criteriaBuilder.createQuery( Owner.class );
					Root<Owner> root = criteria.from( Owner.class );
					criteria.where( criteriaBuilder.equal( root.get( "id" ), o.getId() ) );

					Owner owner = session.createQuery( criteria ).uniqueResult();
//		owner = ( Owner ) s.createCriteria( Owner.class )
//				.add( Restrictions.idEq( owner.getId() ) )
//				.uniqueResult();
					assertThat( owner ).isNotNull();
					assertThat( owner.getAddress() ).isNull();
					session.remove( owner );
				}
		);
	}

	@Test
	public void testNotFoundBidirAssignedId(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Party party = new Party();
					party.partyId = "id";
					party.partyAffiliate = null;
					session.persist( party );
				}
		);

		scope.inTransaction(
				session -> {
					Party party = session.find( Party.class, "id" );
					assertThat( party.partyAffiliate ).isNull();
				}
		);

		scope.inTransaction(
				session -> {
					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<Party> criteria = criteriaBuilder.createQuery( Party.class );
					Root<Party> root = criteria.from( Party.class );
					criteria.where( criteriaBuilder.equal( root.get( "partyId" ), "id" ) );

					Party party = session.createQuery( criteria ).uniqueResult();
//		party = ( Party ) s.createCriteria( Party.class )
//				.add( Restrictions.idEq( "id" ) )
//				.uniqueResult();
					assertThat( party ).isNotNull();
					assertThat( party.partyId ).isEqualTo( "id" );
					assertThat( party.partyAffiliate ).isNull();
					session.remove( party );
				}
		);
	}

}

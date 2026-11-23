package org.hibernate.orm.test.jpa.criteria.components;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Regression coverage for string-based Criteria joins through an embeddable.
///
/// @author Steve Ebersole
@JiraKey("HHH-10600")
@Jpa(annotatedClasses = {
		EmbeddableStringJoinTest.User.class,
		EmbeddableStringJoinTest.Finances.class,
		EmbeddableStringJoinTest.Account.class
})
public class EmbeddableStringJoinTest {
	@Test
	public void testJoinThroughEmbeddable(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Account selectedAccount = new Account();
			final Account otherAccount = new Account();
			entityManager.persist( selectedAccount );
			entityManager.persist( otherAccount );

			final User selectedUser = new User();
			selectedUser.finances.selectedAccount = selectedAccount;
			final User otherUser = new User();
			otherUser.finances.selectedAccount = otherAccount;
			entityManager.persist( selectedUser );
			entityManager.persist( otherUser );
			entityManager.flush();
			entityManager.clear();

			final var builder = entityManager.getCriteriaBuilder();
			final var query = builder.createQuery( User.class );
			final var root = query.from( User.class );
			final var accountJoin = root.join( "finances" ).join( "selectedAccount" );
			query.select( root ).where( builder.equal( accountJoin.get( "id" ), selectedAccount.id ) );

			assertThat( entityManager.createQuery( query ).getResultList() )
					.extracting( user -> user.id )
					.containsExactly( selectedUser.id );
		} );
	}

	@Entity(name = "User")
	@Table(name = "criteria_embeddable_user")
	public static class User {
		@Id
		@GeneratedValue
		Long id;

		@Embedded
		Finances finances = new Finances();
	}

	@Embeddable
	public static class Finances {
		@OneToOne
		Account selectedAccount;
	}

	@Entity(name = "Account")
	@Table(name = "criteria_embeddable_account")
	public static class Account {
		@Id
		@GeneratedValue
		Long id;
	}
}

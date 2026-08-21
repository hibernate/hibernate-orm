/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.graph;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Subgraph;
import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.jpa.SpecHints;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		FetchGraphReadOnlyNpeTest.Account.class,
		FetchGraphReadOnlyNpeTest.Assignment.class,
		FetchGraphReadOnlyNpeTest.UserName.class
})
@SessionFactory
// We need to disable max fetch depth to trigger the original problem
@ServiceRegistry(settings = {@Setting(name = AvailableSettings.MAX_FETCH_DEPTH, value = "")})
@BytecodeEnhanced
@Jira("https://hibernate.atlassian.net/browse/HHH-20251")
public class FetchGraphReadOnlyNpeTest {

	@BeforeAll
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var userName1 = new UserName( 1L );
			final var userName2 = new UserName( 2L );
			session.persist( userName1 );
			session.persist( userName2 );

			final var account = new Account( 1L, userName1 );
			session.persist( account );

			// Assignment 1 references userName2, Assignment 2 references userName1 (same as Account)
			session.persist( new Assignment( 1L, account, userName2 ) );
			session.persist( new Assignment( 2L, account, userName1 ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	void testHibernateBugWithQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var query = session.createQuery( "select a from Account a where a.id = :id", Account.class )
					.setParameter( "id", 1L )
					.setHint( HibernateHints.HINT_READ_ONLY, true )
					.setHint( SpecHints.HINT_SPEC_FETCH_GRAPH, createFetchGraph( session ) );
			final var account = query.getSingleResult();

			assertThat( account.id ).isEqualTo( 1L );

			// userName should be fetched via the graph
			assertThat( Hibernate.isInitialized( account.userName ) ).isTrue();
			assertThat( account.userName.id ).isEqualTo( 1L );

			// assignments should be fetched via the graph
			assertThat( Hibernate.isInitialized( account.assignments ) ).isTrue();
			assertThat( account.assignments ).hasSize( 2 );
			for ( Assignment assignment : account.assignments ) {
				assertThat( Hibernate.isInitialized( assignment.userName ) ).isTrue();
			}
		} );
	}

	private EntityGraph<Account> createFetchGraph(SessionImplementor session) {
		final EntityGraph<Account> fetchGraph = session.createEntityGraph( Account.class );
		fetchGraph.addSubgraph( "userName" );
		final Subgraph<Assignment> assignmentSubgraph = fetchGraph.addSubgraph( "assignments" );
		assignmentSubgraph.addSubgraph( "userName" );
		return fetchGraph;
	}

	@Entity(name = "Account")
	public static class Account {
		@Id
		protected Long id;

		@OneToOne(fetch = FetchType.LAZY)
		private UserName userName;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "account")
		private Set<Assignment> assignments = new HashSet<>();

		public Account() {
		}

		public Account(Long id, UserName userName) {
			this.id = id;
			this.userName = userName;
		}
	}

	@Entity(name = "UserName")
	public static class UserName {
		@Id
		protected Long id;

		@OneToOne(mappedBy = "userName", fetch = FetchType.LAZY)
		private Account account;

		@OneToOne(mappedBy = "userName", fetch = FetchType.LAZY)
		private Assignment assignment;

		public UserName() {
		}

		public UserName(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "Assignment")
	public static class Assignment {
		@Id
		protected Long id;

		@OneToOne
		private UserName userName;

		@ManyToOne
		private Account account;

		public Assignment() {
		}

		public Assignment(Long id, Account account, UserName userName) {
			this.id = id;
			this.account = account;
			this.userName = userName;
		}
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetomany;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Same as {@link org.hibernate.orm.test.mapping.manytomany.ManyToManyCustomSqlMutationsTest ManyToManyCustomSqlMutationsTest}
 * but with {@link OneToMany}
 *
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		OneToManyCustomSqlMutationsTest.Project.class,
		OneToManyCustomSqlMutationsTest.User.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
@SQLDelete( sql = "update t_user set project_id = null where project_id = ? and name = ? and 1=1" )
public class OneToManyCustomSqlMutationsTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final User u1 = new User( "user1" );
			final User u2 = new User( "user2" );
			final Project p1 = new Project( "p1" );
			p1.getMembers().add( u1 );
			p1.getMembers().add( u2 );
			final User u3 = new User( "user3" );
			final Project p2 = new Project( "p2" );
			p2.getMembers().add( u3 );
			p2.getOrderedUsers().add( u2 );
			p2.getOrderedUsers().add( u1 );
			session.persist( u1 );
			session.persist( u2 );
			session.persist( u3 );
			session.persist( p1 );
			session.persist( p2 );
		} );
	}

	@Test
	public void testSQLDelete(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final Project project = session.find( Project.class, "p1" );
			project.getMembers().remove( project.getMembers().iterator().next() );
			inspector.clear();
		} );
		assertThat( inspector.getSqlQueries() ).hasSize( 1 );
		assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "1=1" );
		scope.inTransaction( session -> assertThat(
				session.find( Project.class, "p1" ).getMembers()
		).hasSize( 1 ) );
	}

	@Test
	public void testSQLDeleteAll(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final Project project = session.find( Project.class, "p2" );
			project.getMembers().remove( project.getMembers().iterator().next() );
			inspector.clear();
		} );
		assertThat( inspector.getSqlQueries() ).hasSize( 1 );
		assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "2=2" );
		scope.inTransaction( session -> assertThat(
				session.find( Project.class, "p2" ).getMembers()
		).isEmpty() );
	}

	@Test
	public void testSQLUpdate(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final Project project = session.find( Project.class, "p2" );
			assertThat( project.getOrderedUsers().stream().map( User::getName ) ).containsExactly( "user2", "user1" );
			project.getOrderedUsers().sort( Comparator.comparing( User::getName ) );
			inspector.clear();
		} );
		assertThat( inspector.getSqlQueries() ).hasSize( 4 );
		assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "3=3" );
		assertThat( inspector.getSqlQueries().get( 1 ) ).contains( "3=3" );
		scope.inTransaction( session -> {
			final Project project = session.find( Project.class, "p2" );
			assertThat( project.getOrderedUsers().stream().map( User::getName ) ).containsExactly( "user1", "user2" );
		} );
	}

	@Entity( name = "Project" )
	@Table( name = "t_project" )
	public static class Project {
		@Id
		private String name;

		@OneToMany
		@JoinColumn( name = "project_id" )
		@SQLDelete( sql = "update t_user set project_id = null where project_id = ? and name = ? and 1=1" )
		@SQLDeleteAll( sql = "update t_user set project_id = null where project_id = ? and 2=2" )
		private Set<User> members = new HashSet<>();

		@OneToMany
		@JoinColumn( name = "ordered_project_id" )
		@OrderColumn( name = "order_col" )
		@SQLDelete( sql = "update t_user set project_id = null where project_id = ? and name = ? and 3=3" )
		private List<User> orderedUsers = new ArrayList<>();

		public Project() {
		}

		public Project(String name) {
			this.name = name;
		}

		public Set<User> getMembers() {
			return members;
		}

		public List<User> getOrderedUsers() {
			return orderedUsers;
		}
	}

	@Entity( name = "User" )
	@Table( name = "t_user" )
	public static class User {
		@Id
		private String name;

		public User() {
		}

		public User(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}

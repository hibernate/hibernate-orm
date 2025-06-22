/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.manytomany;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLJoinTableRestriction;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ManyToManySQLJoinTableRestrictionTest.Project.class,
		ManyToManySQLJoinTableRestrictionTest.User.class,
		ManyToManySQLJoinTableRestrictionTest.ProjectUsers.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17105" )
public class ManyToManySQLJoinTableRestrictionTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final User user1 = new User( "user1" );
			final Project project1 = new Project( "p1" );
			project1.getManagers().add( user1 );
			project1.getMembers().add( user1 );
			final Project project2 = new Project( "p2" );
			project2.getMembers().add( user1 );
			session.persist( user1 );
			session.persist( project1 );
			session.persist( project2 );
			final User user2 = new User( "user2" );
			final User user3 = new User( "user3" );
			final Project project3 = new Project( "p3" );
			project3.getMembers().add( user2 );
			project3.getMembers().add( user3 );
			project3.getManagers().add( user2 );
			project3.getOrderedUsers().add(user3);
			project3.getOrderedUsers().add(user2);
			session.persist( user2 );
			session.persist( user3 );
			session.persist( project3 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Project" ).executeUpdate();
			session.createMutationQuery( "delete from User" ).executeUpdate();
		} );
	}

	@Test
	public void testJoinTableRemoveEmptyCollection(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final Project p1 = session.find( Project.class, "p1" );
			p1.getManagers().remove( p1.getManagers().iterator().next() );
			assertThat( p1.getManagers() ).isEmpty();
			inspector.clear();
		} );
		assertThat( inspector.getSqlQueries() ).hasSize( 1 );
		assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "manager" );
		scope.inTransaction( session -> {
			final User user1 = session.find( User.class, "user1" );
			assertThat( user1.getManagedProjects() ).isEmpty();
			assertThat( user1.getOtherProjects().stream().map( Project::getName ) ).contains( "p1", "p2" );
		} );
	}

	@Test
	public void testJoinTableRemoveNonEmptyCollection(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final User user = session.find( User.class, "user2" );
			final Project p3 = session.find( Project.class, "p3" );
			p3.getMembers().remove( user );
			assertThat( p3.getMembers() ).isNotEmpty();
			inspector.clear();
		} );
		assertThat( inspector.getSqlQueries() ).hasSize( 1 );
		assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "member" );
		scope.inTransaction( session -> {
			final User user2 = session.find( User.class, "user2" );
			assertThat( user2.getOtherProjects() ).isEmpty();
			assertThat( user2.getManagedProjects().stream().map( Project::getName ) ).contains( "p3" );
		} );
	}

	@Test
	public void testJoinTableUpdate(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final Project p3 = session.find( Project.class, "p3" );
			assertThat( p3.getOrderedUsers().stream().map( User::getName ) ).containsExactly( "user3", "user2" );
			p3.getOrderedUsers().sort( Comparator.comparing( User::getName ) );
			inspector.clear();
		} );
		assertThat( inspector.getSqlQueries() ).hasSize( 2 );
		assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "order_col is not null" );
		assertThat( inspector.getSqlQueries().get( 1 ) ).contains( "order_col is not null" );
		scope.inTransaction( session -> {
			final Project p3 = session.find( Project.class, "p3" );
			assertThat( p3.getOrderedUsers().stream().map( User::getName ) ).containsExactly( "user2", "user3" );
		} );
	}

	@Entity( name = "Project" )
	@Table( name = "t_project" )
	public static class Project {
		@Id
		private String name;

		@ManyToMany
		@JoinTable(
				name = "project_users",
				joinColumns = { @JoinColumn( name = "project_id" ) },
				inverseJoinColumns = { @JoinColumn( name = "user_id" ) }
		)
		@SQLJoinTableRestriction( "role_name = 'manager'" )
		@SQLInsert( sql = "insert into project_users (project_id, user_id, role_name) values (?, ?, 'manager')" )
		private Set<User> managers = new HashSet<>();

		@ManyToMany
		@JoinTable(
				name = "project_users",
				joinColumns = { @JoinColumn( name = "project_id" ) },
				inverseJoinColumns = { @JoinColumn( name = "user_id" ) }
		)
		@SQLJoinTableRestriction( "role_name = 'member'" )
		@SQLInsert( sql = "insert into project_users (project_id, user_id, role_name) values (?, ?, 'member')" )
		private Set<User> members = new HashSet<>();

		@ManyToMany
		@JoinTable(
				name = "ordered_users",
				joinColumns = { @JoinColumn( name = "project_id" ) },
				inverseJoinColumns = { @JoinColumn( name = "user_id" ) }
		)
		@SQLJoinTableRestriction( "order_col is not null" )
		@OrderColumn( name = "order_col" )
		private List<User> orderedUsers = new ArrayList<>();

		public Project() {
		}

		public Project(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public Set<User> getManagers() {
			return managers;
		}

		public Set<User> getMembers() {
			return members;
		}

		public List<User> getOrderedUsers() {
			return orderedUsers;
		}
	}

	@Entity( name = "ProjectUsers" )
	@Table( name = "project_users" )
	public static class ProjectUsers {
		@Id
		@Column( name = "project_id" )
		private String projectId;

		@Id
		@Column( name = "user_id" )
		private String userId;

		@Id
		@Column( name = "role_name" )
		private String role;
	}

	@Entity( name = "User" )
	@Table( name = "t_user" )
	public static class User {
		@Id
		private String name;

		@ManyToMany( mappedBy = "managers" )
		@SQLJoinTableRestriction( "role_name = 'manager'" )
		private Set<Project> managedProjects = new HashSet<>();

		@ManyToMany( mappedBy = "members" )
		@SQLJoinTableRestriction( "role_name = 'member'" )
		private Set<Project> otherProjects = new HashSet<>();

		public User() {
		}

		public User(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public Set<Project> getManagedProjects() {
			return managedProjects;
		}

		public Set<Project> getOtherProjects() {
			return otherProjects;
		}
	}
}

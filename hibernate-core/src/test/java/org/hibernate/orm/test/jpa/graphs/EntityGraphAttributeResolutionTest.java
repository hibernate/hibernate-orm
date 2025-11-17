/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;

import org.hibernate.graph.GraphSemantic;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Benjamin M.
 * @author Nathan Xu
 */
@JiraKey( value = "HHH-14113" )
@SuppressWarnings({ "unchecked", "rawtypes" })
@Jpa(annotatedClasses = {EntityGraphAttributeResolutionTest.User.class, EntityGraphAttributeResolutionTest.Group.class})
public class EntityGraphAttributeResolutionTest {

	private User u;
	private Group g1, g2;

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			g1 = new Group();
			g1.addPermission( Permission.BAR );
			entityManager.persist( g1 );

			g2 = new Group();
			g2.addPermission( Permission.BAZ );
			entityManager.persist( g2 );

			u = new User();
			entityManager.persist( u );
			u.addGroup( g1 );
			u.addGroup( g2 );
		} );
	}

	@Test
	public void fetchAssocWithNamedFetchGraph(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List result = entityManager.createQuery( "SELECT u.groups FROM User u WHERE u.id = ?1" )
					.setParameter(1, u.getId() )
					.setHint( GraphSemantic.FETCH.getJpaHintName(), entityManager.getEntityGraph( Group.ENTITY_GRAPH ) )
					.getResultList();

			assertThat( result ).containsExactlyInAnyOrder( g1, g2 );
		} );
	}

	@Test
	public void fetchAssocWithNamedFetchGraphAndJoin(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List result = entityManager.createQuery( "SELECT g FROM User u JOIN u.groups g WHERE u.id = ?1" )
					.setParameter( 1, u.getId() )
					.setHint( GraphSemantic.FETCH.getJpaHintName(), entityManager.getEntityGraph( Group.ENTITY_GRAPH ) )
					.getResultList();

			assertThat( result ).containsExactlyInAnyOrder( g1, g2 );
		} );
	}

	@Test
	public void fetchAssocWithAdhocFetchGraph(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			EntityGraph<Group> eg = entityManager.createEntityGraph( Group.class );
			eg.addAttributeNodes( "permissions" );

			List result = entityManager.createQuery( "SELECT u.groups FROM User u WHERE u.id = ?1" )
					.setParameter(1, u.getId() )
					.setHint( GraphSemantic.FETCH.getJpaHintName(), eg )
					.getResultList();

			assertThat( result ).containsExactlyInAnyOrder( g1, g2 );
		} );
	}

	@Entity(name = "Group")
	@NamedEntityGraph(name = Group.ENTITY_GRAPH,
			attributeNodes = {
					@NamedAttributeNode("permissions")
			})
	@Table( name = "t_group") // Name 'group' not accepted by H2
	public static class Group {
		public static final String ENTITY_GRAPH = "group-with-permissions";

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@Enumerated(EnumType.STRING)
		@ElementCollection(targetClass = Permission.class)
		@CollectionTable(
				name = "GROUPS_PERMISSIONS",
				joinColumns = @JoinColumn(name = "gid")
		)
		private Set<Permission> permissions = EnumSet.noneOf( Permission.class );

		public Group() {}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<Permission> getPermissions() {
			return permissions;
		}

		public void setPermissions(Set<Permission> permissions) {
			this.permissions = permissions;
		}

		public void addPermission(Permission p) {
			this.permissions.add( p );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) return true;

			if ( !( o instanceof Group ) )
				return false;

			Group other = (Group) o;

			return id != null &&
					id.equals( other.getId() );
		}

		@Override
		public int hashCode() {
			return 31;
		}

		@Override
		public String toString() {
			return "Group{" +
					"id=" + id +
					'}';
		}
	}

	@Entity(name = "User")
	@Table(name = "users")
	public static class User {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@Enumerated(EnumType.STRING)
		@ElementCollection(targetClass = Permission.class)
		@CollectionTable(name = "USERS_PERMISSIONS", joinColumns = @JoinColumn(name = "user_id"))
		private Set<Permission> permissions = EnumSet.of( Permission.FOO );

		@ManyToMany(fetch = FetchType.LAZY)
		private Set<Group> groups = new HashSet<>();

		public User() {}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<Permission> getPermissions() {
			return permissions;
		}

		public void setPermissions(Set<Permission> permissions) {
			this.permissions = permissions;
		}
		public void addPermission(Permission p) {
			this.permissions.add( p );
		}

		public Set<Group> getGroups() {
			return groups;
		}

		public void setGroups(Set<Group> groups) {
			this.groups = groups;
		}

		public void addGroup(Group g) {
			this.groups.add( g );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) return true;

			if ( !( o instanceof User ) )
				return false;

			User other = (User) o;

			return id != null &&
					id.equals( other.getId() );
		}

		@Override
		public int hashCode() {
			return 31;
		}

		@Override
		public String toString() {
			return "User{" +
					"id=" + id +
					'}';
		}
	}

	public enum Permission {
		FOO, BAR, BAZ
	}
}

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
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;


/**
 * @author Benjamin M.
 * @author Nathan Xu
 */
@JiraKey( value = "HHH-14113" )
@SuppressWarnings({ "unchecked", "rawtypes" })
public class EntityGraphAttributeResolutionTest extends BaseEntityManagerFunctionalTestCase {

	private User u;
	private Group g1, g2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { User.class, Group.class };
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, em -> {
			g1 = new Group();
			g1.addPermission( Permission.BAR );
			em.persist( g1 );

			g2 = new Group();
			g2.addPermission( Permission.BAZ );
			em.persist( g2 );

			u = new User();
			em.persist( u );
			u.addGroup( g1 );
			u.addGroup( g2 );
		} );
	}

	@Test
	public void fetchAssocWithNamedFetchGraph() {
		doInJPA( this::entityManagerFactory, em -> {
			List result = em.createQuery( "SELECT u.groups FROM User u WHERE u.id = ?1" )
					.setParameter(1, u.getId() )
					.setHint( GraphSemantic.FETCH.getJpaHintName(), em.getEntityGraph( Group.ENTITY_GRAPH ) )
					.getResultList();

			assertThat( result ).containsExactlyInAnyOrder( g1, g2 );
		} );
	}

	@Test
	public void fetchAssocWithNamedFetchGraphAndJoin() {
		doInJPA( this::entityManagerFactory, em -> {
			List result = em.createQuery( "SELECT g FROM User u JOIN u.groups g WHERE u.id = ?1" )
					.setParameter( 1, u.getId() )
					.setHint( GraphSemantic.FETCH.getJpaHintName(), em.getEntityGraph( Group.ENTITY_GRAPH ) )
					.getResultList();

			assertThat( result ).containsExactlyInAnyOrder( g1, g2 );
		} );
	}

	@Test
	public void fetchAssocWithAdhocFetchGraph() {
		doInJPA( this::entityManagerFactory, em -> {
			EntityGraph<Group> eg = em.createEntityGraph( Group.class );
			eg.addAttributeNodes( "permissions" );

			List result = em.createQuery( "SELECT u.groups FROM User u WHERE u.id = ?1" )
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

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cdi;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = {
				FetchEmbeddedIdTest.User.class,
				FetchEmbeddedIdTest.GroupType.class,
				FetchEmbeddedIdTest.Group.class,
				FetchEmbeddedIdTest.UserGroup.class
		}
)
@JiraKey( value = "HHH-15875")
public class FetchEmbeddedIdTest {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					User user = new User( 1l, "user name" );

					GroupType groupType = new GroupType( 1l, "group type" );
					Group group = new Group( 1l, "user group", groupType );

					UserGroupId userGroupId = new UserGroupId( user, group );

					UserGroup userGroup = new UserGroup( userGroupId, "value" );

					entityManager.persist( groupType );
					entityManager.persist( group );
					entityManager.persist( user );
					entityManager.persist( userGroup );
				}
		);

	}

	@Test
	public void testCriteriaFetch(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					CriteriaQuery<UserGroup> query = criteriaBuilder.createQuery( UserGroup.class );

					Root<UserGroup> root = query.from( UserGroup.class );

					Fetch<?, ?> userGroupFetch = root.fetch( "userGroupId" );
					userGroupFetch.fetch( "user" );
					userGroupFetch.fetch( "group" ).fetch( "groupType" );

					List<UserGroup> results = entityManager.createQuery( query ).getResultList();
					assertThat( results ).hasSize( 1 );

					UserGroup userGroup = results.get( 0 );
					UserGroupId userGroupId = userGroup.getUserGroupId();
					Group group = userGroupId.getGroup();
					assertTrue( Hibernate.isInitialized( group ) );
					String name = group.getName();
					assertThat( name ).isEqualTo( "user group" );

					User user = userGroupId.getUser();
					assertTrue( Hibernate.isInitialized( user ) );
				}
		);
	}

	@Test
	public void testHqlFetch(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {

					List<UserGroup> results = entityManager.createQuery( "select ug from UserGroup ug join fetch ug.userGroupId ugi join fetch ugi.group join fetch ugi.user" ).getResultList();
					assertThat( results ).hasSize( 1 );

					UserGroup userGroup = results.get( 0 );
					UserGroupId userGroupId = userGroup.getUserGroupId();
					Group group = userGroupId.getGroup();
					assertTrue( Hibernate.isInitialized( group ) );
					String name = group.getName();
					assertThat( name ).isEqualTo( "user group" );

					User user = userGroupId.getUser();
					assertTrue( Hibernate.isInitialized( user ) );
				}
		);
	}

	@Entity(name = "UserGroup")
	public static class UserGroup {

		@EmbeddedId
		private UserGroupId userGroupId;

		private String joinedPropertyValue;

		public UserGroup() {
		}

		public UserGroup(UserGroupId userGroupId, String joinedPropertyValue) {
			this.userGroupId = userGroupId;
			this.joinedPropertyValue = joinedPropertyValue;
		}

		public UserGroupId getUserGroupId() {
			return userGroupId;
		}

		public String getJoinedPropertyValue() {
			return joinedPropertyValue;
		}
	}

	@Embeddable
	public static class UserGroupId implements Serializable {

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		private User user;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		private Group group;

		public UserGroupId() {
		}

		public UserGroupId(User user, Group group) {
			this.user = user;
			this.group = group;
		}

		public User getUser() {
			return user;
		}

		public Group getGroup() {
			return group;
		}

		@Override
		public boolean equals(Object object) {
			if ( this == object ) {
				return true;
			}
			if ( !( object instanceof UserGroupId ) ) {
				return false;
			}

			UserGroupId that = (UserGroupId) object;

			return Objects.equals( user.getId(), that.user.getId() ) && Objects.equals(
					group.getId(),
					that.group.getId()
			);
		}

		@Override
		public int hashCode() {
			return Objects.hash( user.getId(), group.getId() );
		}
	}

	@Entity(name = "User")
	@Table(name = "test_user")
	public static class User {

		@Id
		private Long id;

		private String name;

		public User() {
		}

		public User(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "GROUP")
	@Table(name = "test_group")
	public static class Group {

		@Id
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		private GroupType groupType;

		public Group() {
		}

		public Group(Long id, String name, GroupType groupType) {
			this.id = id;
			this.name = name;
			this.groupType = groupType;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public GroupType getGroupType() {
			return groupType;
		}
	}

	@Entity(name = "GroupType")
	public static class GroupType {

		@Id
		private Long id;

		private String name;

		public GroupType() {

		}

		public GroupType(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.hql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@JiraKey("HHH-20782")
@DomainModel(annotatedClasses = {
		FilterJoinTableManyToManyHqlTest.GroupEntity.class,
		FilterJoinTableManyToManyHqlTest.UserEntity.class
})
@SessionFactory
public class FilterJoinTableManyToManyHqlTest {

	@Test
	void userJoinQueryWithEnabledFilterDoesNotThrowNpe(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.enableFilter("activeGroupAssocFilter");

			List<UserEntity> users = session
					.createQuery("from UserEntity u join u.groups g", UserEntity.class)
					.getResultList();

			assertEquals(0, users.size());
		});
	}

	@Entity(name = "GroupEntity")
	@Table(name = "app_groups")
	@FilterDef(
			name = "activeGroupAssocFilter",
			defaultCondition = "group_id is not null"
	)
	static class GroupEntity {

		@Id
		private Long id;

		private String name;

		@ManyToMany(fetch = FetchType.LAZY)
		@JoinTable(
				name = "user_group",
				joinColumns = @JoinColumn(name = "group_id"),
				inverseJoinColumns = @JoinColumn(name = "user_id")
		)
		@FilterJoinTable(name = "activeGroupAssocFilter")
		private List<UserEntity> users = new ArrayList<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<UserEntity> getUsers() {
			return users;
		}

		public void setUsers(List<UserEntity> users) {
			this.users = users;
		}
	}

	@Entity(name = "UserEntity")
	@Table(name = "app_users")
	static class UserEntity {

		@Id
		private Long id;

		private String username;

		@ManyToMany(mappedBy = "users", fetch = FetchType.LAZY)
		@FilterJoinTable(name = "activeGroupAssocFilter")
		private List<GroupEntity> groups = new ArrayList<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public List<GroupEntity> getGroups() {
			return groups;
		}

		public void setGroups(List<GroupEntity> groups) {
			this.groups = groups;
		}
	}
}

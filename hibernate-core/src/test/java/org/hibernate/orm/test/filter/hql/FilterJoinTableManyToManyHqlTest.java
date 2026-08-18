/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.hql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.ParamDef;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
		FilterJoinTableManyToManyHqlTest.UserEntity.class,
		FilterJoinTableManyToManyHqlTest.MasterEntity.class
})
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.DRIVER, value = "org.h2.Driver"),
		@Setting(name = AvailableSettings.URL, value = "jdbc:h2:mem:filter_join_table_bug;DB_CLOSE_DELAY=-1"),
		@Setting(name = AvailableSettings.USER, value = "sa"),
		@Setting(name = AvailableSettings.PASS, value = ""),
		@Setting(name = AvailableSettings.HBM2DDL_AUTO, value = "none")
})
@SessionFactory(exportSchema = false)
public class FilterJoinTableManyToManyHqlTest {

	private static final String JDBC_URL = "jdbc:h2:mem:filter_join_table_bug;DB_CLOSE_DELAY=-1";

	@BeforeAll
	static void setUpSchema() throws Exception {
		createSchema();
	}

	@AfterAll
	static void tearDownSchema() throws Exception {
		try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "");
				Statement statement = connection.createStatement()) {
			statement.execute("drop all objects");
		}
	}

	@BeforeEach
	void seedData(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.createNativeMutationQuery("delete from group_master").executeUpdate();
			session.createNativeMutationQuery("delete from user_group").executeUpdate();
			session.createNativeMutationQuery("delete from app_masters").executeUpdate();
			session.createNativeMutationQuery("delete from app_groups").executeUpdate();
			session.createNativeMutationQuery("delete from app_users").executeUpdate();

			session.createNativeMutationQuery("insert into app_groups (id, name) values (1, 'Group-A')")
					.executeUpdate();
			session.createNativeMutationQuery("insert into app_users (id, username) values (1, 'active-user')")
					.executeUpdate();
			session.createNativeMutationQuery("insert into app_users (id, username) values (2, 'inactive-user')")
					.executeUpdate();
			session.createNativeMutationQuery("insert into app_masters (id, name) values (1, 'Master-Active')")
					.executeUpdate();
			session.createNativeMutationQuery("insert into app_masters (id, name) values (2, 'Master-Inactive')")
					.executeUpdate();
			session.createNativeMutationQuery("insert into app_masters (id, name) values (3, 'Master-Active-2')")
					.executeUpdate();
			session.createNativeMutationQuery("insert into app_masters (id, name) values (4, 'Master-Active-3')")
					.executeUpdate();

			session.createNativeMutationQuery(
					"insert into user_group (group_id, user_id, is_active) values (1, 1, true)").executeUpdate();
			session.createNativeMutationQuery(
					"insert into user_group (group_id, user_id, is_active) values (1, 2, false)").executeUpdate();
			session.createNativeMutationQuery(
					"insert into group_master (group_id, master_id, is_active) values (1, 1, true)").executeUpdate();
			session.createNativeMutationQuery(
					"insert into group_master (group_id, master_id, is_active) values (1, 2, false)").executeUpdate();
			session.createNativeMutationQuery(
					"insert into group_master (group_id, master_id, is_active) values (1, 3, true)").executeUpdate();
			session.createNativeMutationQuery(
					"insert into group_master (group_id, master_id, is_active) values (1, 4, true)").executeUpdate();
		});
	}

	@Test
	void filterJoinTableReturnsOnlyActiveAssociations(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.enableFilter("activeGroupAssocFilter").setParameter("activeOnly", true);

			List<GroupEntity> groups = session.createQuery("from GroupEntity g order by g.id", GroupEntity.class)
					.getResultList();

			assertEquals(1, groups.size());
			assertEquals(Set.of("active-user"), usernames(groups.get(0).getUsers()));
		});
	}

	@Test
	void filterJoinTableReturnsOnlyInactiveAssociations(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.enableFilter("activeGroupAssocFilter").setParameter("activeOnly", false);

			List<GroupEntity> groups = session.createQuery("from GroupEntity g order by g.id", GroupEntity.class)
					.getResultList();

			assertEquals(1, groups.size());
			assertEquals(Set.of("inactive-user"), usernames(groups.get(0).getUsers()));
		});
	}

	@Test
	void withoutFilterAllAssociationsAreVisible(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			List<GroupEntity> groups = session.createQuery("from GroupEntity g order by g.id", GroupEntity.class)
					.getResultList();

			assertEquals(1, groups.size());
			assertEquals(Set.of("active-user", "inactive-user"), usernames(groups.get(0).getUsers()));
		});
	}

	@Test
	void userQueryWithoutFilterAllAssociationsAreVisible(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			List<UserEntity> users = session.createQuery("from UserEntity u order by u.id", UserEntity.class)
					.getResultList();

			assertEquals(2, users.size());
			assertEquals(1, users.get(0).getGroups().size());
			assertEquals(1, users.get(1).getGroups().size());
		});
	}

	@Test
	void userQueryWithActiveFilterReturnsOnlyActiveAssociations(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.enableFilter("activeGroupAssocFilter").setParameter("activeOnly", true);

			List<UserEntity> users = session.createQuery("from UserEntity u order by u.id", UserEntity.class)
					.getResultList();

			assertEquals(2, users.size());
			assertEquals(1, users.get(0).getGroups().size());
			assertEquals(0, users.get(1).getGroups().size());
		});
	}

	@Test
	void userQueryWithInactiveFilterReturnsOnlyInactiveAssociations(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.enableFilter("activeGroupAssocFilter").setParameter("activeOnly", false);

			List<UserEntity> users = session.createQuery("from UserEntity u order by u.id", UserEntity.class)
					.getResultList();

			assertEquals(2, users.size());
			assertEquals(0, users.get(0).getGroups().size());
			assertEquals(1, users.get(1).getGroups().size());
		});
	}

	@Test
	void userJoinQueryWithActiveFilterReturnsOnlyActiveAssociations(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.enableFilter("activeGroupAssocFilter").setParameter("activeOnly", true);

			List<UserEntity> users = session
					.createQuery("from UserEntity u join u.groups g order by g.id", UserEntity.class).getResultList();
			assertEquals(1, users.size());

			List<UserGroupDto> dtos = session.createQuery(
					"select new org.hibernate.orm.test.filter.hql.FilterJoinTableManyToManyHqlTest$UserGroupDto(u.id, u.username, g.id)"
							+ " from UserEntity u join u.groups g order by g.id",
					UserGroupDto.class).getResultList();
			assertEquals(1, dtos.size());
		});
	}

	@Test
	void userJoinQueryWithInactiveFilterReturnsOnlyInactiveAssociations(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.enableFilter("activeGroupAssocFilter").setParameter("activeOnly", false);

			List<UserEntity> users = session
					.createQuery("from UserEntity u join u.groups g order by g.id", UserEntity.class).getResultList();
			assertEquals(1, users.size());

			List<UserGroupDto> dtos = session.createQuery(
					"select new org.hibernate.orm.test.filter.hql.FilterJoinTableManyToManyHqlTest$UserGroupDto(u.id, u.username, g.id)"
							+ " from UserEntity u join u.groups g order by g.id",
					UserGroupDto.class).getResultList();
			assertEquals(1, dtos.size());
		});
	}

	@Test
	void userJoinQueryWithoutActiveFilterReturnsAllAssociations(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			List<UserEntity> users = session
					.createQuery("from UserEntity u join u.groups g order by g.id", UserEntity.class).getResultList();
			assertEquals(2, users.size());

			List<UserGroupDto> dtos = session.createQuery(
					"select new org.hibernate.orm.test.filter.hql.FilterJoinTableManyToManyHqlTest$UserGroupDto(u.id, u.username, g.id)"
							+ " from UserEntity u join u.groups g order by g.id",
					UserGroupDto.class).getResultList();
			assertEquals(2, dtos.size());
		});
	}

	@Test
	void userGroupMasterJoinQueryWithActiveFilterReturnsOnlyActiveAssociations(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.enableFilter("activeGroupAssocFilter").setParameter("activeOnly", true);

			List<UserEntity> users = session
					.createQuery("from UserEntity u join u.groups g join g.masters m order by m.id", UserEntity.class)
					.getResultList();
			// Hibernate deduplicates entities: 1 distinct active user (spans 3 active master rows)
			assertEquals(1, users.size());
			assertEquals(1, users.get(0).getGroups().size());

			// DTO projection returns 1 active user × 3 active masters = 3 rows (no deduplication)
			List<UserGroupMasterDto> dtos = session.createQuery(
					"select new org.hibernate.orm.test.filter.hql.FilterJoinTableManyToManyHqlTest$UserGroupMasterDto(u.id, u.username, g.id, m.id)"
							+ " from UserEntity u join u.groups g join g.masters m order by m.id",
					UserGroupMasterDto.class).getResultList();
			assertEquals(3, dtos.size());
		});
	}

	@Test
	void userGroupMasterJoinQueryWithInactiveFilterReturnsOnlyInactiveAssociations(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			session.enableFilter("activeGroupAssocFilter").setParameter("activeOnly", false);

			List<UserEntity> users = session
					.createQuery("from UserEntity u join u.groups g join g.masters m order by m.id", UserEntity.class)
					.getResultList();
			// 1 inactive user × 1 inactive master = 1 row
			assertEquals(1, users.size());

			List<UserGroupMasterDto> dtos = session.createQuery(
					"select new org.hibernate.orm.test.filter.hql.FilterJoinTableManyToManyHqlTest$UserGroupMasterDto(u.id, u.username, g.id, m.id)"
							+ " from UserEntity u join u.groups g join g.masters m order by m.id",
					UserGroupMasterDto.class).getResultList();
			assertEquals(1, dtos.size());
		});
	}

	@Test
	void userGroupMasterJoinQueryWithoutActiveFilterReturnsAllAssociations(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			List<UserEntity> users = session
					.createQuery("from UserEntity u join u.groups g join g.masters m order by m.id", UserEntity.class)
					.getResultList();
			// Hibernate deduplicates entities: 2 distinct users (spans 2 users × 4 masters = 8 rows)
			assertEquals(2, users.size());

			// DTO projection returns 2 users × 4 masters = 8 rows (no deduplication)
			List<UserGroupMasterDto> dtos = session.createQuery(
					"select new org.hibernate.orm.test.filter.hql.FilterJoinTableManyToManyHqlTest$UserGroupMasterDto(u.id, u.username, g.id, m.id)"
							+ " from UserEntity u join u.groups g join g.masters m order by m.id",
					UserGroupMasterDto.class).getResultList();
			assertEquals(8, dtos.size());
		});
	}

	private static Set<String> usernames(List<UserEntity> users) {
		Set<String> names = new HashSet<>();
		for (UserEntity user : users) {
			names.add(user.getUsername());
		}
		return names;
	}

	private static void createSchema() throws Exception {
		try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "");
				Statement statement = connection.createStatement()) {
			statement.execute("create table app_groups (id bigint primary key, name varchar(64) not null)");
			statement.execute("create table app_users (id bigint primary key, username varchar(64) not null)");
			statement.execute("create table app_masters (id bigint primary key, name varchar(64) not null)");
			statement.execute(
					"create table user_group (group_id bigint not null, user_id bigint not null, is_active boolean not null, primary key (group_id, user_id))"
			);
			statement.execute(
					"create table group_master (group_id bigint not null, master_id bigint not null, is_active boolean not null, primary key (group_id, master_id))"
			);
			statement.execute(
					"alter table user_group add constraint fk_group foreign key (group_id) references app_groups(id)"
			);
			statement.execute(
					"alter table user_group add constraint fk_user foreign key (user_id) references app_users(id)"
			);
			statement.execute(
					"alter table group_master add constraint fk_group_master_group foreign key (group_id) references app_groups(id)"
			);
			statement.execute(
					"alter table group_master add constraint fk_group_master_master foreign key (master_id) references app_masters(id)"
			);
		}
	}

	static class UserGroupDto {
		private final Long userId;
		private final String username;
		private final Long groupId;

		public UserGroupDto(Long userId, String username, Long groupId) {
			this.userId = userId;
			this.username = username;
			this.groupId = groupId;
		}

		public Long getUserId() {
			return userId;
		}

		public String getUsername() {
			return username;
		}

		public Long getGroupId() {
			return groupId;
		}
	}

	static class UserGroupMasterDto {
		private final Long userId;
		private final String username;
		private final Long groupId;
		private final Long masterId;

		public UserGroupMasterDto(Long userId, String username, Long groupId, Long masterId) {
			this.userId = userId;
			this.username = username;
			this.groupId = groupId;
			this.masterId = masterId;
		}

		public Long getUserId() {
			return userId;
		}

		public String getUsername() {
			return username;
		}

		public Long getGroupId() {
			return groupId;
		}

		public Long getMasterId() {
			return masterId;
		}
	}

	@Entity(name = "GroupEntity")
	@Table(name = "app_groups")
	@FilterDef(
			name = "activeGroupAssocFilter",
			parameters = @ParamDef(name = "activeOnly", type = Boolean.class),
			defaultCondition = "is_active = :activeOnly"
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
		@FilterJoinTable(name = "activeGroupAssocFilter", condition = "is_active = :activeOnly")
		private List<UserEntity> users = new ArrayList<>();

		@ManyToMany(fetch = FetchType.LAZY)
		@JoinTable(
				name = "group_master",
				joinColumns = @JoinColumn(name = "group_id"),
				inverseJoinColumns = @JoinColumn(name = "master_id")
		)
		@FilterJoinTable(name = "activeGroupAssocFilter", condition = "is_active = :activeOnly")
		private List<MasterEntity> masters = new ArrayList<>();

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

		public List<MasterEntity> getMasters() {
			return masters;
		}

		public void setMasters(List<MasterEntity> masters) {
			this.masters = masters;
		}
	}

	@Entity(name = "UserEntity")
	@Table(name = "app_users")
	static class UserEntity {

		@Id
		private Long id;

		private String username;

		@ManyToMany(mappedBy = "users", fetch = FetchType.LAZY)
		@FilterJoinTable(name = "activeGroupAssocFilter", condition = "is_active = :activeOnly")
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

	@Entity(name = "MasterEntity")
	@Table(name = "app_masters")
	static class MasterEntity {

		@Id
		private Long id;

		private String name;

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
	}
}

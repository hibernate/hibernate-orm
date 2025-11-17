/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.fetch.depth;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				DepthOneTest.Agency.class,
				DepthOneTest.AgencyDetail.class,
				DepthOneTest.User.class,
				DepthOneTest.Group.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.MAX_FETCH_DEPTH, value = "1"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "false")
		}
)
@RequiresDialect(H2Dialect.class)
@JiraKey("HHH-16469")
public class DepthOneTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Agency agency = new Agency( 1, "Test Agency" );

					AgencyDetail agencyDetail = new AgencyDetail( 1, "abc" );
					agency.setAgencyDetail( agencyDetail );
					session.persist( agencyDetail );
					session.persist( agency );
					Group group = new Group( 1, "Test Group 1" );

					agency.addGroup( group );
					session.persist( group );
					for ( int i = 1; i < 9; i++ ) {
						User user = new User( i, "User " + i );
						group.addUser( user );
						agency.addUser( user );
						session.persist( user );
					}
				}
		);
	}

	@Test
	public void tesGetAgency(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					Agency agency = session.get( Agency.class, 1 );
					assertThat( agency ).isNotNull();
					assertThat( agency.getAgencyDetail() ).isNotNull();

					Set<Group> groups = agency.getGroups();
					assertThat( groups.size() ).isEqualTo( 1 );

					Group group = groups.iterator().next();
					assertThat( group.getAgency() ).isNotNull();
					assertThat( group.getAgency() ).isSameAs( agency );

					Set<User> users = group.getUsers();
					assertThat( users.size() ).isEqualTo( 8 );
					for ( User user : users ) {
						assertThat( user.getAgency() ).isNotNull();
						assertThat( user.getAgency() ).isSameAs( agency );
					}

					List<String> executedQueries = statementInspector.getSqlQueries();
					assertThat( executedQueries.size() ).isEqualTo( 4 );

					assertThat( executedQueries.get( 0 ).toLowerCase() ).isEqualTo(
							"select a1_0.agency_id,ad1_0.agency_id,ad1_0.agency_detail,a1_0.agency_txt from agency_table a1_0 left join agency_detail_table ad1_0 on ad1_0.agency_id=a1_0.agency_id where a1_0.agency_id=?"
					);

					assertThat( executedQueries.get( 1 ).toLowerCase() ).isEqualTo(
							"select u1_0.agency_id,u1_0.user_id,u1_0.user_name from user_table u1_0 where u1_0.agency_id=?"
					);

					assertThat( executedQueries.get( 2 ).toLowerCase() ).isEqualTo(
							"select g1_0.agency_id,g1_0.group_id,g1_0.group_name from group_table g1_0 where g1_0.agency_id=?"
					);

					assertThat( executedQueries.get( 3 ).toLowerCase() ).isEqualTo(
							"select u1_0.group_id,u1_1.user_id,a1_0.agency_id,a1_0.agency_txt,u1_1.user_name from group_user u1_0 join user_table u1_1 on u1_1.user_id=u1_0.user_id left join agency_table a1_0 on a1_0.agency_id=u1_1.agency_id where u1_0.group_id=?"
					);
				}
		);
	}

	@Entity(name = "Agency")
	@Table(name = "AGENCY_TABLE")
	public static class Agency {

		private Integer agencyId;
		private String agencyName;

		private AgencyDetail agencyDetail;

		private Set<User> users;

		private Set<Group> groups;

		public Agency() {
		}

		public Agency(Integer agencyId, String agencyName) {
			this.agencyId = agencyId;
			this.agencyName = agencyName;
		}

		@Id
		@Column(name = "AGENCY_ID")
		public Integer getAgencyId() {
			return agencyId;
		}

		public void setAgencyId(Integer agencyId) {
			this.agencyId = agencyId;
		}

		@Column(name = "AGENCY_TXT")
		public String getAgencyName() {
			return agencyName;
		}

		public void setAgencyName(String agencyName) {
			this.agencyName = agencyName;
		}

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@PrimaryKeyJoinColumn(name = "AGENCY_ID")
		public AgencyDetail getAgencyDetail() {
			return agencyDetail;
		}

		public void setAgencyDetail(AgencyDetail agencyDetail) {
			this.agencyDetail = agencyDetail;
		}

		@OneToMany(
				cascade = CascadeType.ALL,
				orphanRemoval = true,
				fetch = FetchType.EAGER,
				mappedBy = "agency")
		@Fetch(FetchMode.SELECT)
		public Set<User> getUsers() {
			return users;
		}

		public void setUsers(Set<User> users) {
			this.users = users;
		}

		public void addUser(User user) {
			if ( users == null ) {
				users = new HashSet<>();
			}
			users.add( user );
			user.setAgency( this );
		}

		@OneToMany(
				cascade = CascadeType.ALL,
				orphanRemoval = true,
				fetch = FetchType.EAGER,
				mappedBy = "agency")
		@Fetch(FetchMode.SELECT)
		public Set<Group> getGroups() {
			return groups;
		}

		public void setGroups(Set<Group> groups) {
			this.groups = groups;
		}

		public void addGroup(Group group) {
			if ( groups == null ) {
				groups = new HashSet<>();
			}
			groups.add( group );
			group.setAgency( this );
		}
	}

	@Entity(name = "AgencyDetail")
	@Table(name = "AGENCY_DETAIL_TABLE")
	public static class AgencyDetail {

		private Integer agencyId = null;
		private String agencyDetail = null;

		public AgencyDetail() {
		}

		public AgencyDetail(Integer agencyId, String agencyDetail) {
			this.agencyId = agencyId;
			this.agencyDetail = agencyDetail;
		}

		@Id
		@Column(name = "AGENCY_ID")
		public Integer getAgencyId() {
			return agencyId;
		}

		public void setAgencyId(Integer agencyId) {
			this.agencyId = agencyId;
		}

		@Column(name = "AGENCY_DETAIL")
		public String getAgencyDetail() {
			return agencyDetail;
		}

		public void setAgencyDetail(String agencyDetail) {
			this.agencyDetail = agencyDetail;
		}
	}

	@Entity(name = "User")
	@Table(name = "USER_TABLE")
	public static class User {

		private Integer userId;
		private String userName;

		private Agency agency;

		public User() {
		}

		public User(Integer userId, String userName) {
			this.userId = userId;
			this.userName = userName;
		}

		@Id
		@Column(name = "USER_ID")
		public Integer getUserId() {
			return userId;
		}

		public void setUserId(Integer userId) {
			this.userId = userId;
		}

		@Column(name = "USER_NAME")
		public String getUserName() {
			return userName;
		}

		public void setUserName(String userName) {
			this.userName = userName;
		}

		@ManyToOne
		@JoinColumn(name = "AGENCY_ID")
		public Agency getAgency() {
			return agency;
		}

		public void setAgency(Agency agency) {
			this.agency = agency;
		}

	}

	@Entity(name = "Group")
	@Table(name = "GROUP_TABLE")
	public static class Group {

		private Integer groupId;
		private String name;
		private Agency agency;
		private Set<User> users;

		public Group() {
		}

		public Group(Integer groupId, String name) {
			this.groupId = groupId;
			this.name = name;
		}

		@Id
		@Column(name = "GROUP_ID")
		public Integer getGroupId() {
			return groupId;
		}

		public void setGroupId(Integer groupId) {
			this.groupId = groupId;
		}

		@Column(name = "GROUP_NAME", nullable = false, unique = false)
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
		@JoinTable(
				name = "GROUP_USER",
				joinColumns = @JoinColumn(name = "GROUP_ID"),
				inverseJoinColumns = @JoinColumn(name = "USER_ID"))
		@Fetch(FetchMode.SUBSELECT)
		public Set<User> getUsers() {
			return users;
		}

		public void setUsers(Set<User> users) {
			this.users = users;
		}

		public void addUser(User user) {
			if ( users == null ) {
				users = new HashSet<>();
			}
			users.add( user );
		}

		@ManyToOne
		@JoinColumn(name = "AGENCY_ID")
		public Agency getAgency() {
			return agency;
		}

		public void setAgency(Agency agency) {
			this.agency = agency;
		}
	}

}

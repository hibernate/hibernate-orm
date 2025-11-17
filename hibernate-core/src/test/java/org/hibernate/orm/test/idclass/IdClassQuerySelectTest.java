/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				IdClassQuerySelectTest.Organisation.class,
				IdClassQuerySelectTest.User.class,
				IdClassQuerySelectTest.OrganisationUser.class,
		}
)
@SessionFactory
@JiraKey(value = "HHH-16366")
public class IdClassQuerySelectTest {

	private static final Integer ORGANISATION_ID = 1;
	private static final Integer USER_ID = 2;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Organisation organisation = new Organisation( ORGANISATION_ID, "Red Hat" );
					session.persist( organisation );

					User user = new User( USER_ID, AccountType.FOO );
					session.persist( user );

					OrganisationUser organisationUser = new OrganisationUser( organisation, user, "1" );
					session.persist( organisationUser );
				}
		);
	}

	@Test
	public void testSelectUsingIdClassInWhereClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<OrganisationUser> resultList = session.createQuery(
									"select distinct o from OrganisationUser o join fetch o.user u where o.organisation.id = ?1 and u.accountType = ?2 ",
									OrganisationUser.class
							)
							.setParameter( 1, ORGANISATION_ID )
							.setParameter( 2, AccountType.FOO )
							.getResultList();

					assertThat( resultList.size() ).isEqualTo( 1 );
				}
		);

		scope.inTransaction(
				session -> {
					List<OrganisationUser> resultList = session.createQuery(
									"select distinct o from OrganisationUser o join fetch o.user u join fetch o.organisation or where or.id = ?1 and u.accountType = ?2 ",
									OrganisationUser.class
							)
							.setParameter( 1, ORGANISATION_ID )
							.setParameter( 2, AccountType.FOO )
							.getResultList();

					assertThat( resultList.size() ).isEqualTo( 1 );
				}
		);

		scope.inTransaction(
				session -> {
					List<OrganisationUser> resultList = session.createQuery(
									"select distinct o from OrganisationUser o join fetch o.organisation or where or.id = ?1 and o.user.accountType = ?2 ",
									OrganisationUser.class
							)
							.setParameter( 1, ORGANISATION_ID )
							.setParameter( 2, AccountType.FOO )
							.getResultList();

					assertThat( resultList.size() ).isEqualTo( 1 );
				}
		);

		scope.inTransaction(
				session -> {
					List<OrganisationUser> resultList = session.createQuery(
									"select distinct o from OrganisationUser o where o.organisation.id = ?1 and o.user.accountType = ?2 ",
									OrganisationUser.class
							)
							.setParameter( 1, ORGANISATION_ID )
							.setParameter( 2, AccountType.FOO )
							.getResultList();

					assertThat( resultList.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<OrganisationUser> resultList = session.createQuery(
									"select distinct o from OrganisationUser o",
									OrganisationUser.class
							)
							.getResultList();

					assertThat( resultList.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testSelectJoiningPartOfIdClass(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<OrganisationUser> resultList = session.createQuery(
									"select distinct o from OrganisationUser o join fetch o.user u  ",
									OrganisationUser.class
							)
							.getResultList();

					assertThat( resultList.size() ).isEqualTo( 1 );
				}
		);
	}

	@Entity(name = "OrganisationUser")
	@Table(name = "ORGANISATION_USER")
	@IdClass(OrganisationUserId.class)
	public static class OrganisationUser {

		@Id
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "ORGANISATION_ID", nullable = false)
		private Organisation organisation;

		@Id
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "USER_ID", nullable = false)
		private User user;

		public OrganisationUser() {
		}

		public OrganisationUser(Organisation organisation, User user, String code) {
			this.organisation = organisation;
			this.user = user;
			this.code = code;
		}

		private String code;

		public Organisation getOrganisation() {
			return organisation;
		}

		public User getUser() {
			return user;
		}
	}

	@Entity(name = "User")
	@Table(name = "F_USER")
	public static class User {

		@Id
		private Integer id;

		@Enumerated(EnumType.STRING)
		@Column(name = "ACCOUNT_TYPE")
		private AccountType accountType;

		public User() {
		}

		public User(Integer id, AccountType accountType) {
			this.id = id;
			this.accountType = accountType;
		}

		public Integer getId() {
			return id;
		}

		public AccountType getAccountType() {
			return accountType;
		}

	}

	@Entity(name = "Organisation")
	@Table(name = "ORGANISATION")
	public static class Organisation {

		@Id
		@Column(name = "ID", unique = true)
		private Integer id;

		private String name;

		public Organisation() {
		}

		public Organisation(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	public enum AccountType {
		FOO,
		BAR,
	}

	public static class OrganisationUserId implements Serializable {

		private Integer organisation;
		private Integer user;

		public OrganisationUserId() {
		}

		public OrganisationUserId(Integer organisation, Integer user) {
			this.organisation = organisation;
			this.user = user;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			OrganisationUserId that = (OrganisationUserId) o;
			return Objects.equals( organisation, that.organisation ) && Objects.equals( user, that.user );
		}

		@Override
		public int hashCode() {
			return Objects.hash( organisation, user );
		}

		public Integer getOrganisation() {
			return organisation;
		}

		public void setOrganisation(Integer organisation) {
			this.organisation = organisation;
		}

		public Integer getUser() {
			return user;
		}

		public void setUser(Integer user) {
			this.user = user;
		}
	}

}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				EmbeddableQuerySelectTest.Organisation.class,
				EmbeddableQuerySelectTest.User.class,
				EmbeddableQuerySelectTest.OrganisationUser.class,
		}
)
@SessionFactory
@JiraKey(value = "HHH-16366")
public class EmbeddableQuerySelectTest {

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

					OrganisationUserEmbeddable embeddable = new OrganisationUserEmbeddable( organisation, user, "1" );

					OrganisationUser organisationUser = new OrganisationUser( 3, embeddable );
					session.persist( organisationUser );
				}
		);
	}

	@Test
	public void testSelectUsingEmbeddableInWhereClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<OrganisationUser> resultList = session.createQuery(
									"select distinct o from OrganisationUser o join fetch o.embeddable.user u where o.embeddable.organisation.id = ?1 and u.accountType = ?2 ",
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
									"select distinct o from OrganisationUser o join fetch o.embeddable.user u join fetch o.embeddable.organisation or where or.id = ?1 and u.accountType = ?2 ",
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
									"select distinct o from OrganisationUser o join fetch o.embeddable.organisation or where or.id = ?1 and o.embeddable.user.accountType = ?2 ",
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
									"select distinct o from OrganisationUser o where o.embeddable.organisation.id = ?1 and o.embeddable.user.accountType = ?2 ",
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
	public void testSelectJoiningPartOfEmbeddable(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<OrganisationUser> resultList = session.createQuery(
									"select distinct o from OrganisationUser o join fetch o.embeddable.user u  ",
									OrganisationUser.class
							)
							.getResultList();

					assertThat( resultList.size() ).isEqualTo( 1 );
				}
		);
	}

	@Entity(name = "OrganisationUser")
	@Table(name = "ORGANISATION_USER")
	public static class OrganisationUser {

		@Id
		private Integer id;

		private OrganisationUserEmbeddable embeddable;

		public OrganisationUser() {
		}

		public OrganisationUser(Integer id, OrganisationUserEmbeddable organisationUserEmbeddable) {
			this.id = id;
			this.embeddable = organisationUserEmbeddable;
		}

		public Integer getId() {
			return id;
		}

		public OrganisationUserEmbeddable getEmbeddable() {
			return embeddable;
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

	@Embeddable
	public static class OrganisationUserEmbeddable {

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "ORGANISATION_ID", nullable = false)
		private Organisation organisation;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "USER_ID", nullable = false)
		private User user;

		private String code;

		public OrganisationUserEmbeddable() {
		}

		public OrganisationUserEmbeddable(Organisation organisation, User user, String code) {
			this.organisation = organisation;
			this.user = user;
			this.code = code;
		}

		public Organisation getOrganisation() {
			return organisation;
		}

		public User getUser() {
			return user;
		}

		public String getCode() {
			return code;
		}
	}

}

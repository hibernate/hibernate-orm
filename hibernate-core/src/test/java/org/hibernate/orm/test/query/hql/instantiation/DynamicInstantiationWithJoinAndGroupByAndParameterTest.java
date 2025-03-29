/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.instantiation;


import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@Jpa(annotatedClasses = {
		DynamicInstantiationWithJoinAndGroupByAndParameterTest.Action.class,
		DynamicInstantiationWithJoinAndGroupByAndParameterTest.UserEntity.class
})
@JiraKey("HHH-15991")
public class DynamicInstantiationWithJoinAndGroupByAndParameterTest {
	private static final String PARTNER_NUMBER = "1111111111";

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final UserEntity user1 = new UserEntity( "John", "Tester" );
			entityManager.persist( user1 );
			entityManager.persist( new Action( PARTNER_NUMBER, "Test 1", user1 ) );
			entityManager.persist( new Action( PARTNER_NUMBER, "Test 2", user1 ) );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "delete from Action" ).executeUpdate();
			entityManager.createQuery( "delete from UserEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testIt(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			TypedQuery<UserStatistic> query = entityManager.createQuery(
					"select new " + getClass().getName() + "$UserStatistic(u, count(a))" +
							" from Action a inner join a.user u" +
							" where a.partnerNumber = :partnerNumber" +
							" group by u",
					UserStatistic.class
			);
			query.setParameter( "partnerNumber", PARTNER_NUMBER );
			UserStatistic result = query.getSingleResult();
			assertEquals( "John Tester", result.getName() );
			assertEquals( 2, result.getCount() );
		} );
	}

	@Entity(name = "UserEntity")
	@Table(name = "UserEntity")
	public static class UserEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String firstname;

		private String lastname;

		public UserEntity() {
		}

		public UserEntity(String firstname, String lastname) {
			this.firstname = firstname;
			this.lastname = lastname;
		}

		public Long getId() {
			return id;
		}

		public String getFirstname() {
			return firstname;
		}

		public void setFirstname(String firstname) {
			this.firstname = firstname;
		}

		public String getLastname() {
			return lastname;
		}

		public void setLastname(String lastname) {
			this.lastname = lastname;
		}
	}

	@Entity(name = "Action")
	@Table(name = "Action")
	public static class Action {
		@Id
		@GeneratedValue
		private Long id;

		private String partnerNumber;

		private String title;

		@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
		@JoinColumn(name = "fk_user_id")
		private UserEntity user;

		public Action() {
		}

		public Action(String partnerNumber, String title, UserEntity user) {
			this.partnerNumber = partnerNumber;
			this.title = title;
			this.user = user;
		}

		public Long getId() {
			return id;
		}

		public String getPartnerNumber() {
			return partnerNumber;
		}

		public void setPartnerNumber(String partnerNumber) {
			this.partnerNumber = partnerNumber;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public UserEntity getUser() {
			return user;
		}

		public void setUser(UserEntity user) {
			this.user = user;
		}
	}

	public static class UserStatistic {
		private final String name;

		private final Long count;

		public UserStatistic(UserEntity user, Long count) {
			this.name = user != null ? user.getFirstname() + " " + user.getLastname() : null;
			this.count = count;
		}

		public String getName() {
			return name;
		}

		public Integer getCount() {
			return count.intValue();
		}
	}
}

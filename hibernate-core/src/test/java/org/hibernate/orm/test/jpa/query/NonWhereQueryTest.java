/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import java.util.Objects;
import java.util.UUID;

import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

/**
 * @author Jan Schatteman
 */
@JiraKey( value = "HHH-15257" )
@Jpa(
		annotatedClasses = {
				NonWhereQueryTest.TestUser.class
		}
)
public class NonWhereQueryTest {

	@BeforeAll
	public void prepareTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					TestUser user = new TestUser();
					user.setLoggedIn( true );
					entityManager.persist( user );
				}
		);
	}

	@AfterAll
	public void cleanupTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> entityManager.createQuery( "delete from TestUser").executeUpdate()
		);
	}

	@Test
	public void testNonWhereQueryOnJoinInheritedTable(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					int i = entityManager.createQuery( "update TestUser x set x.loggedIn = false" ).executeUpdate();
					Assertions.assertEquals( 1, i);
				}
		);
	}

	@Entity(name = "TestUser")
	public static class TestUser extends AbstractEntity {

		@Column
		private Boolean loggedIn;

		public TestUser() {
			super();
		}

		public boolean isLoggedIn() {
			if (this.loggedIn == null) {
				return false;
			}
			return this.loggedIn;
		}

		public void setLoggedIn(boolean loggedIn) {
			this.loggedIn = loggedIn;
		}
	}

	@Entity(name = "AbstractEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class AbstractEntity implements Comparable<AbstractEntity> {

		private final UUID uuid;

		@Id
		@GeneratedValue
		private int id;

		public int getId() {
			return this.id;
		}

		public UUID getUuid() {
			return this.uuid;
		}

		public AbstractEntity() {
			super();
			this.uuid = SafeRandomUUIDGenerator.safeRandomUUID();
		}

		@Override
		public boolean equals(Object obj) {
			int usedId = this.getId();
			if (usedId > 0) {
				return (obj instanceof AbstractEntity) && (usedId == ((AbstractEntity) obj).getId());
			}
			return super.equals(obj);
		}

		@Override
		public int compareTo(AbstractEntity o) {
			return Integer.compare(this.getId(), o.getId());
		}

		@Override
		public int hashCode() {
			final int usedId = this.getId();
			if (usedId > 0) {
				return Objects.hash( this.getClass().toString(), usedId);
			}
			return super.hashCode();
		}
	}

}

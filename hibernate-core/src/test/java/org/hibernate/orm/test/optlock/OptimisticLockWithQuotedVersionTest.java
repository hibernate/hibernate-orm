/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.optlock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


@DomainModel(
		annotatedClasses = OptimisticLockWithQuotedVersionTest.Person.class
)
@SessionFactory
public class OptimisticLockWithQuotedVersionTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person person = new Person( "1", "Fabiana" );
					session.persist( person );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testHqlQueryWithOptimisticLock(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from Person e", Person.class )
							.setLockMode( LockModeType.OPTIMISTIC )
							.getResultList().get( 0 );
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private String id;

		@Version
		@Column(name = "`version`")
		private long version;

		private String name;

		public Person() {
		}

		public Person(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
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

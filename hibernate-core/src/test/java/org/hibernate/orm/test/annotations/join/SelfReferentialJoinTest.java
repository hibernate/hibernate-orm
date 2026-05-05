/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.join;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = SelfReferentialJoinTest.Person2.class)
@SessionFactory
public class SelfReferentialJoinTest {
	@BeforeEach
	void prepareTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Person2 adam2 = new Person2(1, "Adam", null );
			Person2 bob2 = new Person2(2, "Bob", adam2 );
			adam2.backUp = bob2;
			session.persist( adam2 );
			session.persist( bob2 );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();

	}

	@Test
	void testDeletionHandling2(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var bob2 = session.get(Person2.class, 2);
			session.remove( bob2.supervisor );
			session.remove( bob2 );
		} );
	}

	@Entity
	@Table(name = "person2")
	public static class Person2 {
		@Id
		Integer id;
		String name;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "backup_fk")
		Person2 backUp;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "supervisor_fk")
		Person2 supervisor;  // Self-referential FK on secondary table!

		public Person2() {
		}

		public Person2(Integer id, String name, Person2 supervisor) {
			this.id = id;
			this.name = name;
			this.supervisor = supervisor;
		}
	}

}

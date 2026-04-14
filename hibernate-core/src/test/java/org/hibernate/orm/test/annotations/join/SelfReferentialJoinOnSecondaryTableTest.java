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
import jakarta.persistence.SecondaryTable;
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
@DomainModel(annotatedClasses = SelfReferentialJoinOnSecondaryTableTest.Person.class)
@SessionFactory
public class SelfReferentialJoinOnSecondaryTableTest {
	@BeforeEach
	void prepareTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Person adam = new Person(1, "Adam", null );
			Person bob = new Person(2, "Bob", adam );
			adam.backUp = bob;
			session.persist( adam );
			session.persist( bob );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();

	}

	@Test
	void testDeletionHandling(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var bob = session.get(Person.class, 2);
			session.remove( bob.supervisor );
			session.remove( bob );
		} );
	}

	@Entity
	@Table(name = "person")
	@SecondaryTable(name = "org_chart")
	public static class Person {
		@Id
		Integer id;
		String name;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "backup_fk", table = "org_chart")
		Person backUp;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "supervisor_fk", table = "org_chart")
		Person supervisor;  // Self-referential FK on secondary table!

		public Person() {
		}

		public Person(Integer id, String name, Person supervisor) {
			this.id = id;
			this.name = name;
			this.supervisor = supervisor;
		}
	}
}

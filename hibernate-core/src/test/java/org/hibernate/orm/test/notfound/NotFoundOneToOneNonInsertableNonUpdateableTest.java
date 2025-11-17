/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.notfound;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				NotFoundOneToOneNonInsertableNonUpdateableTest.Person.class,
				NotFoundOneToOneNonInsertableNonUpdateableTest.PersonInfo.class
		}
)
@SessionFactory
public class NotFoundOneToOneNonInsertableNonUpdateableTest {
	private static final int ID = 1;

	@Test
	public void testOneToOne(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					Person person = new Person();
					person.id = ID;
					person.personInfo = new PersonInfo();
					person.personInfo.id = ID;
					session.persist( person );
				}
		);

		scope.inTransaction(
				session ->
						session.remove( session.get( PersonInfo.class, ID ) )
		);

		scope.inTransaction(
				session -> {
					Person person = session.get( Person.class, ID );
					assertNotNull( person );
					assertNull( person.personInfo );

					session.remove( person );
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private int id;

		@OneToOne(optional = true, cascade = CascadeType.ALL)
		@JoinColumn(
				name = "id",
				updatable = false,
				insertable = false
		)
		@NotFound(action = NotFoundAction.IGNORE)
		private PersonInfo personInfo;
	}

	@Entity(name = "PersonInfo")
	public static class PersonInfo {
		@Id
		private int id;

	}
}

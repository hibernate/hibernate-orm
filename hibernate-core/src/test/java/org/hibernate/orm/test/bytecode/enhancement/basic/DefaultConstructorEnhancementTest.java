/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

@BytecodeEnhanced
@SessionFactory
@DomainModel(annotatedClasses = DefaultConstructorEnhancementTest.Person.class )
public class DefaultConstructorEnhancementTest {

	@Test
	public void testDefaultConstructorAdded() {
		// Check if the default constructor exists for Person
		try {
			Constructor<Person> constructor = Person.class.getConstructor();
			Assertions.assertNotNull( constructor );
		}
		catch (NoSuchMethodException e) {
			Assertions.fail( "Default constructor should have been added to Person by bytecode enhancement" );
		}
	}

	@Test
	public void testPersistAndLoad(SessionFactoryScope scope) {
		Person person = new Person( "Gavin" );
		person.id = 1L;
		scope.inTransaction( session -> {
			session.persist( person );
		} );

		scope.inTransaction( session -> {
			Person loadedPerson = session.get( Person.class, 1L );
			Assertions.assertNotNull( loadedPerson );
			Assertions.assertEquals( "Gavin", loadedPerson.name );
		} );
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		Long id;

		String name;

		// No default constructor
		public Person(String name) {
			this.name = name;
		}
	}

}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.joinedsubclass;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Steve Ebersole
 */
@JiraKey(value = "HHH-6911")
@DomainModel(
		annotatedClasses = {
				JoinedSubclassWithExplicitDiscriminatorTest.Animal.class,
				JoinedSubclassWithExplicitDiscriminatorTest.Cat.class,
				JoinedSubclassWithExplicitDiscriminatorTest.Dog.class
		}
)
@SessionFactory
public class JoinedSubclassWithExplicitDiscriminatorTest {

	@Test
	@SkipForDialect( dialectClass = AltibaseDialect.class, reason = "'TYPE' is a keyword in Altibase and escaped here")
	public void metadataAssertions(SessionFactoryScope scope) {
		EntityPersister p = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor(Dog.class.getName());
		assertNotNull( p );
		final JoinedSubclassEntityPersister dogPersister = assertTyping( JoinedSubclassEntityPersister.class, p );
		assertEquals( "string", dogPersister.getDiscriminatorType().getName() );
		assertEquals( "type", dogPersister.getDiscriminatorColumnName() );
		assertEquals( "dog", dogPersister.getDiscriminatorValue() );

		p = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor(Cat.class.getName());
		assertNotNull( p );
		final JoinedSubclassEntityPersister catPersister = assertTyping( JoinedSubclassEntityPersister.class, p );
		assertEquals( "string", catPersister.getDiscriminatorType().getName() );
		assertEquals( "type", catPersister.getDiscriminatorColumnName() );
		assertEquals( "cat", catPersister.getDiscriminatorValue() );
	}

	@Test
	public void basicUsageTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new Cat( 1 ) );
					session.persist( new Dog( 2 ) );
				}
		);

		scope.inTransaction(
				session -> {
					session.createQuery( "from Animal" ).list();
					Cat cat = session.get( Cat.class, 1 );
					assertNotNull( cat );
					session.remove( cat );
					Dog dog = session.get( Dog.class, 2 );
					assertNotNull( dog );
					session.remove( dog );
				}
		);
	}

	public <T> T assertTyping(Class<T> expectedType, Object value) {
		if ( ! expectedType.isInstance( value ) ) {
			fail(
					String.format(
							"Expecting value of type [%s], but found [%s]",
							expectedType.getName(),
							value == null ? "<null>" : value
					)
			);
		}
		return (T) value;
	}

	@Entity(name = "Animal")
	@Table(name = "animal")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
	@DiscriminatorValue(value = "???animal???")
	public static abstract class Animal {
		@Id
		public Integer id;

		protected Animal() {
		}

		protected Animal(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Cat")
	@DiscriminatorValue(value = "cat")
	public static class Cat extends Animal {
		public Cat() {
			super();
		}

		public Cat(Integer id) {
			super( id );
		}
	}

	@Entity(name = "Dog")
	@DiscriminatorValue(value = "dog")
	public static class Dog extends Animal {
		public Dog() {
			super();
		}

		public Dog(Integer id) {
			super( id );
		}
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.joinedsubclass;

import org.hibernate.boot.model.internal.AnnotatedDiscriminatorColumn;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@JiraKey(value = "HHH-6911")
@DomainModel(
		annotatedClasses = {
				JoinedSubclassWithImplicitDiscriminatorTest.Animal.class,
				JoinedSubclassWithImplicitDiscriminatorTest.Cat.class,
				JoinedSubclassWithImplicitDiscriminatorTest.Dog.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings =
		@Setting(
				name = AvailableSettings.IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS,
				value = "true"))
public class JoinedSubclassWithImplicitDiscriminatorTest {

	@Test
	public void metadataAssertions(SessionFactoryScope scope) {
		EntityPersister p = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor(Dog.class.getName());
		assertNotNull( p );
		final JoinedSubclassEntityPersister dogPersister = assertTyping( JoinedSubclassEntityPersister.class, p );
		assertEquals(
				AnnotatedDiscriminatorColumn.DEFAULT_DISCRIMINATOR_TYPE,
				dogPersister.getDiscriminatorType().getName()
		);
		assertEquals(
				AnnotatedDiscriminatorColumn.DEFAULT_DISCRIMINATOR_COLUMN_NAME,
				dogPersister.getDiscriminatorColumnName()
		);
		assertEquals( "Dog", dogPersister.getDiscriminatorValue() );

		p = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor(Cat.class.getName());
		assertNotNull( p );
		final JoinedSubclassEntityPersister catPersister = assertTyping( JoinedSubclassEntityPersister.class, p );
		assertEquals(
				AnnotatedDiscriminatorColumn.DEFAULT_DISCRIMINATOR_TYPE,
				catPersister.getDiscriminatorType().getName()
		);
		assertEquals(
				AnnotatedDiscriminatorColumn.DEFAULT_DISCRIMINATOR_COLUMN_NAME,
				catPersister.getDiscriminatorColumnName()
		);
		assertEquals( "Cat", catPersister.getDiscriminatorValue() );
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
		if ( !expectedType.isInstance( value ) ) {
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
	public static class Cat extends Animal {
		public Cat() {
			super();
		}

		public Cat(Integer id) {
			super( id );
		}
	}

	@Entity(name = "Dog")
	public static class Dog extends Animal {
		public Dog() {
			super();
		}

		public Dog(Integer id) {
			super( id );
		}
	}
}

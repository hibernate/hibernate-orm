package org.hibernate.test.joinedsubclass;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Ejb3DiscriminatorColumn;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-6911" )
public class JoinedSubclassWithImplicitDiscriminatorTest extends BaseCoreFunctionalTestCase {
	@Entity( name = "Animal" )
	@Table( name = "animal" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static abstract class Animal {
		@Id
		public Integer id;

		protected Animal() {
		}

		protected Animal(Integer id) {
			this.id = id;
		}
	}

	@Entity( name = "Cat" )
	public static class Cat extends Animal {
		public Cat() {
			super();
		}

		public Cat(Integer id) {
			super( id );
		}
	}

	@Entity( name = "Dog" )
	public static class Dog extends Animal {
		public Dog() {
			super();
		}

		public Dog(Integer id) {
			super( id );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Animal.class, Cat.class, Dog.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS, "true" );
	}

	@Test
	@FailureExpectedWithNewMetamodel
	public void metadataAssertions() {
		EntityPersister p = sessionFactory().getEntityPersister( Dog.class.getName() );
		assertNotNull( p );
		final JoinedSubclassEntityPersister dogPersister = assertTyping( JoinedSubclassEntityPersister.class, p );
		assertEquals( Ejb3DiscriminatorColumn.DEFAULT_DISCRIMINATOR_TYPE, dogPersister.getDiscriminatorType().getName() );
		assertEquals( Ejb3DiscriminatorColumn.DEFAULT_DISCRIMINATOR_COLUMN_NAME, dogPersister.getDiscriminatorColumnName() );
		assertEquals( "Dog", dogPersister.getDiscriminatorValue() );

		p = sessionFactory().getEntityPersister( Cat.class.getName() );
		assertNotNull( p );
		final JoinedSubclassEntityPersister catPersister = assertTyping( JoinedSubclassEntityPersister.class, p );
		assertEquals( Ejb3DiscriminatorColumn.DEFAULT_DISCRIMINATOR_TYPE, catPersister.getDiscriminatorType().getName() );
		assertEquals( Ejb3DiscriminatorColumn.DEFAULT_DISCRIMINATOR_COLUMN_NAME, catPersister.getDiscriminatorColumnName() );
		assertEquals( "Cat", catPersister.getDiscriminatorValue() );
	}

	@Test
	@FailureExpectedWithNewMetamodel( jiraKey = "HHH-9050", message = "The WrongCLassException stuff")
	public void basicUsageTest() {
		Session session = openSession();
		session.beginTransaction();
		session.save( new Cat( 1 ) );
		session.save( new Dog( 2 ) );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.createQuery( "from Animal" ).list();
		Cat cat = (Cat) session.get( Cat.class, 1 );
		assertNotNull( cat );
		session.delete( cat );
		Dog dog = (Dog) session.get( Dog.class, 2 );
		assertNotNull( dog );
		session.delete( dog );
		session.getTransaction().commit();
		session.close();
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-13640" )
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true,inlineDirtyChecking = true)
public class ProxyInitializeAndUpdateInlineDirtyTrackingTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applyStatisticsSupport( true );
		sfb.applySecondLevelCacheSupport( false );
		sfb.applyQueryCacheSupport( false );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( Animal.class );
	}

	@Test
	public void testInitializeWithGetter() {
		inTransaction(
				session -> {
					Animal animal = new Animal();
					animal.name = "animal";
					animal.age = 3;
					animal.sex = "female";
					animal.color = "green";
					session.persist( animal );
				}
		);

		inTransaction(
				session -> {
					Animal animal = session.load( Animal.class, "animal" );
					assertFalse( Hibernate.isInitialized( animal ) );
					assertEquals( "female", animal.getSex() );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( 3, animal.getAge() );
					animal.setSex( "other" );
				}
		);

		inSession(
				session -> {
					Animal animal = session.get( Animal.class, "animal" );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( "other", animal.getSex() );
					assertEquals( 3, animal.getAge() );
					assertEquals( "green", animal.getColor() );
				}
		);
	}

	@Test
	public void testInitializeWithSetter() {
		inTransaction(
				session -> {
					Animal animal = new Animal();
					animal.name = "animal";
					animal.age = 3;
					animal.sex = "female";
					animal.color = "green";
					session.persist( animal );
				}
		);

		inTransaction(
				session -> {
					Animal animal = session.load( Animal.class, "animal" );
					assertFalse( Hibernate.isInitialized( animal ) );
					animal.setSex( "other" );
					// Setting the attribute value should have initialized animal.
					assertTrue( Hibernate.isInitialized( animal ) );
				}
		);

		inSession(
				session -> {
					Animal animal = session.get( Animal.class, "animal" );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( "other", animal.getSex() );
					assertEquals( "green", animal.getColor() );
					assertEquals( 3, animal.getAge() );
				}
		);
	}

	@Test
	public void testMergeUpdatedOntoUninitialized() {
		inTransaction(
				session -> {
					Animal animal = new Animal();
					animal.name = "animal";
					animal.age = 3;
					animal.sex = "female";
					session.persist( animal );
				}
		);

		final Animal animalInitialized = doInHibernate(
				this::sessionFactory,
				session -> {
					final Animal animal = session.get( Animal.class, "animal" );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( "female", animal.getSex() );
					assertEquals( 3, animal.getAge() );
					return animal;
				}
		);

		animalInitialized.setAge( 4 );
		animalInitialized.setSex( "other" );

		inTransaction(
				session -> {
					final Animal animal = session.load( Animal.class, "animal" );
					assertFalse( Hibernate.isInitialized( animal ) );
					session.merge( animalInitialized );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( 4, animal.getAge() );
					assertEquals( "other", animal.getSex() );
				}
		);

		inTransaction(
				session -> {
					final Animal animal = session.get( Animal.class, "animal" );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( "other", animal.getSex() );
					assertEquals( 4, animal.getAge() );
				}
		);
	}

	@Test
	public void testMergeUpdatedOntoUpdated() {
		inTransaction(
				session -> {
					Animal animal = new Animal();
					animal.name = "animal";
					animal.age = 3;
					animal.sex = "female";
					session.persist( animal );
				}
		);

		final Animal animalInitialized = doInHibernate(
				this::sessionFactory,
				session -> {
					final Animal animal = session.get( Animal.class, "animal" );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( "female", animal.getSex() );
					assertEquals( 3, animal.getAge() );
					return animal;
				}
		);

		animalInitialized.setAge( 4 );
		animalInitialized.setSex( "other" );

		inTransaction(
				session -> {
					final Animal animal = session.get( Animal.class, "animal" );
					assertTrue( Hibernate.isInitialized( animal ) );
					animal.setAge( 5 );
					animal.setSex( "male" );
					session.merge( animalInitialized );
					assertEquals( 4, animal.getAge() );
					assertEquals( "other", animal.getSex() );
				}
		);

		inTransaction(
				session -> {
					final Animal animal = session.get( Animal.class, "animal" );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( "other", animal.getSex() );
					assertEquals( 4, animal.getAge() );
				}
		);
	}

	@Test
	public void testMergeUninitializedOntoUninitialized() {
		inTransaction(
				session -> {
					Animal animal = new Animal();
					animal.name = "animal";
					animal.age = 3;
					animal.sex = "female";
					session.persist( animal );
				}
		);

		final Animal animalUninitialized = doInHibernate(
				this::sessionFactory,
				session -> {
					final Animal animal = session.load( Animal.class, "animal" );
					assertFalse( Hibernate.isInitialized( animal ) );
					return animal;
				}
		);

		inTransaction(
				session -> {
					final Animal animal = session.load( Animal.class, "animal" );
					assertFalse( Hibernate.isInitialized( animal ) );
					session.merge( animalUninitialized );
					assertFalse( Hibernate.isInitialized( animal ) );
				}
		);

		inTransaction(
				session -> {
					final Animal animal = session.get( Animal.class, "animal" );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( "female", animal.getSex() );
					assertEquals( 3, animal.getAge() );
				}
		);
	}

	@Test
	public void testMergeUninitializedOntoUpdated() {
		inTransaction(
				session -> {
					Animal animal = new Animal();
					animal.name = "animal";
					animal.age = 3;
					animal.sex = "female";
					session.persist( animal );
				}
		);

		final Animal animalUninitialized = doInHibernate(
				this::sessionFactory,
				session -> {
					final Animal animal = session.load( Animal.class, "animal" );
					assertFalse( Hibernate.isInitialized( animal ) );
					return animal;
				}
		);

		inTransaction(
				session -> {
					final Animal animal = session.get( Animal.class, "animal" );
					assertTrue( Hibernate.isInitialized( animal ) );
					animal.setSex( "other" );
					animal.setAge( 4 );
					session.merge( animalUninitialized );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( "other", animal.getSex() );
					assertEquals( 4, animal.getAge() );
				}
		);

		inTransaction(
				session -> {
					final Animal animal = session.get( Animal.class, "animal" );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( "other", animal.getSex() );
					assertEquals( 4, animal.getAge() );
				}
		);
	}

	@After
	public void cleanUpTestData() {
		inTransaction(
				session -> {
					session.createQuery( "delete from Animal" ).executeUpdate();
				}
		);
	}

	@Entity(name = "Animal")
	@Table(name = "Animal")
	public static class Animal {

		@Id
		private String name;

		private int age;

		private String sex;

		private String color;

		public String getName() {
			return name;
		}

		protected void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public String getSex() {
			return sex;
		}

		public void setSex(String sex) {
			this.sex = sex;
		}

		public String getColor() {
			return color;
		}

		public void setColor(String color) {
			this.color = color;
		}
	}
}
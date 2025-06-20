/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.spi.LoadState;

import org.hibernate.Hibernate;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.internal.util.PersistenceUtilHelper;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Gail Badner
 */
@JiraKey( "HHH-13640" )
@DomainModel(
		annotatedClasses = {
				ProxyInitializeAndUpdateInlineDirtyTrackingDynamicUpdateTest.Animal.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "false" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true,inlineDirtyChecking = true)
public class ProxyInitializeAndUpdateInlineDirtyTrackingDynamicUpdateTest {

	private final PersistenceUtilHelper.MetadataCache metadataCache = new PersistenceUtilHelper.MetadataCache();

	@Test
	public void testInitializeWithGetter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Animal animal = new Animal();
					animal.name = "animal";
					animal.age = 3;
					animal.sex = "female";
					animal.color = "green";
					session.persist( animal );
				}
		);

		scope.inTransaction(
				session -> {
					Animal animal = session.getReference( Animal.class, "animal" );
					assertFalse( Hibernate.isInitialized( animal ) );
					assertEquals( LoadState.NOT_LOADED , PersistenceUtilHelper.isLoadedWithoutReference( animal, "sex", metadataCache ) );
					assertEquals( "female", animal.getSex() );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( LoadState.LOADED , PersistenceUtilHelper.isLoadedWithoutReference( animal, "sex", metadataCache ) );
					assertEquals( 3, animal.getAge() );
					animal.setSex( "other" );
				}
		);

		scope.inSession(
				session -> {
					Animal animal = session.get( Animal.class, "animal" );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( LoadState.LOADED , PersistenceUtilHelper.isLoadedWithoutReference( animal, "name", metadataCache ) );
					assertEquals( "other", animal.getSex() );
					assertEquals( 3, animal.getAge() );
					assertEquals( "green", animal.getColor() );
				}
		);
	}

	@Test
	public void testInitializeWithSetter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Animal animal = new Animal();
					animal.name = "animal";
					animal.age = 3;
					animal.sex = "female";
					animal.color = "green";
					session.persist( animal );
				}
		);

		scope.inTransaction(
				session -> {
					Animal animal = session.getReference( Animal.class, "animal" );
					assertFalse( Hibernate.isInitialized( animal ) );
					assertEquals( LoadState.NOT_LOADED , PersistenceUtilHelper.isLoadedWithoutReference( animal, "sex", metadataCache ) );
					animal.setSex( "other" );
					// Setting the attribute value should not initialize animal
					// with dirty-checking and dynamic-update.
					assertFalse( Hibernate.isInitialized( animal ) );
					assertEquals( LoadState.NOT_LOADED , PersistenceUtilHelper.isLoadedWithoutReference( animal, "sex", metadataCache ) );
				}
		);

		scope.inSession(
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
	public void testMergeUpdatedOntoUninitialized(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Animal animal = new Animal();
					animal.name = "animal";
					animal.age = 3;
					animal.sex = "female";
					session.persist( animal );
				}
		);

		final Animal animalInitialized = scope.fromTransaction( session -> {
					final Animal animal = session.get( Animal.class, "animal" );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( "female", animal.getSex() );
					assertEquals( 3, animal.getAge() );
					return animal;
				}
		);

		animalInitialized.setAge( 4 );
		animalInitialized.setSex( "other" );

		scope.inTransaction(
				session -> {
					final Animal animal = session.getReference( Animal.class, "animal" );
					assertFalse( Hibernate.isInitialized( animal ) );
					assertEquals( LoadState.NOT_LOADED , PersistenceUtilHelper.isLoadedWithoutReference( animal, "age", metadataCache ) );
					session.merge( animalInitialized );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( LoadState.LOADED , PersistenceUtilHelper.isLoadedWithoutReference( animal, "age", metadataCache ) );
					assertEquals( 4, animal.getAge() );
					assertEquals( "other", animal.getSex() );
				}
		);

		scope.inTransaction(
				session -> {
					final Animal animal = session.get( Animal.class, "animal" );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( "other", animal.getSex() );
					assertEquals( 4, animal.getAge() );
				}
		);
	}

	@Test
	public void testMergeUpdatedOntoUpdated(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Animal animal = new Animal();
					animal.name = "animal";
					animal.age = 3;
					animal.sex = "female";
					session.persist( animal );
				}
		);

		final Animal animalInitialized = scope.fromTransaction( session -> {
					final Animal animal = session.get( Animal.class, "animal" );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( "female", animal.getSex() );
					assertEquals( 3, animal.getAge() );
					return animal;
				}
		);

		animalInitialized.setAge( 4 );
		animalInitialized.setSex( "other" );

		scope.inTransaction(
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

		scope.inTransaction(
				session -> {
					final Animal animal = session.get( Animal.class, "animal" );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( "other", animal.getSex() );
					assertEquals( 4, animal.getAge() );
				}
		);
	}

	@Test
	public void testMergeUninitializedOntoUninitialized(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Animal animal = new Animal();
					animal.name = "animal";
					animal.age = 3;
					animal.sex = "female";
					session.persist( animal );
				}
		);

		final Animal animalUninitialized = scope.fromTransaction( session -> {
					final Animal animal = session.getReference( Animal.class, "animal" );
					assertFalse( Hibernate.isInitialized( animal ) );
					assertEquals( LoadState.NOT_LOADED , PersistenceUtilHelper.isLoadedWithoutReference( animal, "age", metadataCache ) );
					assertFalse( Hibernate.isInitialized( animal ) ); //checking again against side effects of using PersistenceUtilHelper
					return animal;
				}
		);

		scope.inTransaction(
				session -> {
					final Animal animal = session.getReference( Animal.class, "animal" );
					assertFalse( Hibernate.isInitialized( animal ) );
					assertEquals( LoadState.NOT_LOADED , PersistenceUtilHelper.isLoadedWithoutReference( animal, "age", metadataCache ) );
					session.merge( animalUninitialized );
					assertFalse( Hibernate.isInitialized( animal ) );
					assertEquals( LoadState.NOT_LOADED , PersistenceUtilHelper.isLoadedWithoutReference( animal, "age", metadataCache ) );
					assertFalse( Hibernate.isInitialized( animal ) );
				}
		);

		scope.inTransaction(
				session -> {
					final Animal animal = session.get( Animal.class, "animal" );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( "female", animal.getSex() );
					assertEquals( 3, animal.getAge() );
				}
		);
	}

	@Test
	public void testMergeUninitializedOntoUpdated(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Animal animal = new Animal();
					animal.name = "animal";
					animal.age = 3;
					animal.sex = "female";
					session.persist( animal );
				}
		);

		final Animal animalUninitialized = scope.fromTransaction( session -> {
					final Animal animal = session.getReference( Animal.class, "animal" );
					assertFalse( Hibernate.isInitialized( animal ) );
					assertEquals( LoadState.NOT_LOADED , PersistenceUtilHelper.isLoadedWithoutReference( animal, "age", metadataCache ) );
					assertFalse( Hibernate.isInitialized( animal ) );
					return animal;
				}
		);

		scope.inTransaction(
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

		scope.inTransaction(
				session -> {
					final Animal animal = session.get( Animal.class, "animal" );
					assertTrue( Hibernate.isInitialized( animal ) );
					assertEquals( "other", animal.getSex() );
					assertEquals( 4, animal.getAge() );
				}
		);
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Animal")
	@Table(name = "Animal")
	@DynamicUpdate
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

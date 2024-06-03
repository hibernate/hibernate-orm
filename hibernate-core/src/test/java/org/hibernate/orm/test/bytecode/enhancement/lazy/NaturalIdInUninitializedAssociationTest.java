/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.annotations.NaturalId;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */
@JiraKey( "HHH-13607" )
@DomainModel(
		annotatedClasses = {
				NaturalIdInUninitializedAssociationTest.AnEntity.class,
				NaturalIdInUninitializedAssociationTest.EntityMutableNaturalId.class,
				NaturalIdInUninitializedAssociationTest.EntityImmutableNaturalId.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.FORMAT_SQL, value = "false" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "false" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions( lazyLoading = true, extendedEnhancement = true )
public class NaturalIdInUninitializedAssociationTest {

	@Test
	public void testLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final AnEntity e = session.byId( AnEntity.class ).load(3 );
					assertTrue( Hibernate.isInitialized( e ) );

					// because we can (enhanced) proxy both EntityMutableNaturalId and EntityImmutableNaturalId
					// we will select their FKs and all attributes will be "bytecode initialized"
					assertTrue( Hibernate.isPropertyInitialized( e,"entityMutableNaturalId" ) );
					assertTrue( Hibernate.isPropertyInitialized( e,"entityImmutableNaturalId" ) );

					assertEquals( "mutable name", e.entityMutableNaturalId.name );
					assertEquals( "immutable name", e.entityImmutableNaturalId.name );
				}
		);
	}

	@Test
	public void testGetReference(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final AnEntity e = session.byId( AnEntity.class ).getReference( 3 );
					assertFalse( Hibernate.isInitialized( e ) );

					// trigger initialization
					Hibernate.initialize( e );
					// silly, but...
					assertTrue( Hibernate.isInitialized( e ) );

					// because we can (enhanced) proxy both EntityMutableNaturalId and EntityImmutableNaturalId
					// we will select their FKs and all attributes will be "bytecode initialized"
					assertTrue( Hibernate.isPropertyInitialized( e,"entityMutableNaturalId" ) );
					assertTrue( Hibernate.isPropertyInitialized( e,"entityImmutableNaturalId" ) );
				}
		);

		scope.inTransaction(
				session -> {
					final AnEntity e = session.get( AnEntity.class, 3 );
					assertEquals( "mutable name", e.entityMutableNaturalId.name );
				}
		);
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityMutableNaturalId entityMutableNaturalId = new EntityMutableNaturalId( 1, "mutable name" );
					EntityImmutableNaturalId entityImmutableNaturalId = new EntityImmutableNaturalId( 2, "immutable name" );
					AnEntity anEntity = new AnEntity();
					anEntity.id = 3;
					anEntity.entityImmutableNaturalId = entityImmutableNaturalId;
					anEntity.entityMutableNaturalId = entityMutableNaturalId;
					session.persist( anEntity );
				}
		);
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.delete( session.get( AnEntity.class, 3 ) );
				}
		);
	}

	@Entity(name = "AnEntity")
	public static class AnEntity {
		@Id
		private int id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		@LazyToOne(LazyToOneOption.NO_PROXY )
		private EntityMutableNaturalId entityMutableNaturalId;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		@LazyToOne(LazyToOneOption.NO_PROXY )
		private EntityImmutableNaturalId entityImmutableNaturalId;
	}

	@Entity(name = "EntityMutableNaturalId")
	public static class EntityMutableNaturalId {
		@Id
		private int id;

		@NaturalId(mutable = true)
		private String name;

		public EntityMutableNaturalId() {
		}

		public EntityMutableNaturalId(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "EntityImmutableNaturalId")
	public static class EntityImmutableNaturalId {
		@Id
		private int id;

		@NaturalId(mutable = false)
		private String name;

		public EntityImmutableNaturalId() {
		}

		public EntityImmutableNaturalId(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}

}

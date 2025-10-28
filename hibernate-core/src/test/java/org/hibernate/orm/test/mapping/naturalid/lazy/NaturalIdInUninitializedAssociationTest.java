/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.lazy;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.annotations.NaturalId;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Gail Badner
 */
@JiraKey( value = "HHH-13607" )
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.FORMAT_SQL, value = "false" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "false" ),
		}
)
@DomainModel(
		annotatedClasses = {
				NaturalIdInUninitializedAssociationTest.AnEntity.class,
				NaturalIdInUninitializedAssociationTest.EntityMutableNaturalId.class,
				NaturalIdInUninitializedAssociationTest.EntityImmutableNaturalId.class
		}
)
@SessionFactory
public class NaturalIdInUninitializedAssociationTest {

	@Test
	public void testImmutableNaturalId(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final AnEntity e = session.find( AnEntity.class, 3 );
					assertFalse( Hibernate.isInitialized( e.entityImmutableNaturalId ) );
				}
		);

		scope.inTransaction(
				(session) -> {
					final AnEntity e = session.find( AnEntity.class, 3 );
					Hibernate.initialize( e.entityImmutableNaturalId );
					assertEquals( "immutable name", e.entityImmutableNaturalId.getName() );
				}
		);
	}

	@Test
	public void testMutableNaturalId(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final AnEntity e = session.find( AnEntity.class, 3 );
					assertFalse( Hibernate.isInitialized( e.entityMutableNaturalId ) );
				}
		);

		scope.inTransaction(
				(session) -> {
					final AnEntity e = session.find( AnEntity.class, 3 );
					assertEquals( "mutable name", e.entityMutableNaturalId.getName() );
				}
		);
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
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
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "AnEntity")
	public static class AnEntity {
		@Id
		private int id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		private EntityMutableNaturalId entityMutableNaturalId;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
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

		public String getName() {
			return name;
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

		public String getName() {
			return name;
		}
	}

}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Hibernate;
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

/**
 * @author Gail Badner
 */
@JiraKey( "HHH-13607" )
@DomainModel(
		annotatedClasses = {
				NaturalIdInUninitializedProxyTest.EntityMutableNaturalId.class,
				NaturalIdInUninitializedProxyTest.EntityImmutableNaturalId.class
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
@EnhancementOptions( lazyLoading = true )
public class NaturalIdInUninitializedProxyTest {

	@Test
	public void testImmutableNaturalId(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityImmutableNaturalId e = session.getReference( EntityImmutableNaturalId.class, 1 );
					assertFalse( Hibernate.isInitialized( e ) );
				}
		);

		scope.inTransaction(
				session -> {
					final EntityImmutableNaturalId e = session.get( EntityImmutableNaturalId.class, 1 );
					assertEquals( "name", e.name );
				}
		);
	}

	@Test
	public void testMutableNaturalId(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityMutableNaturalId e = session.getReference( EntityMutableNaturalId.class, 1 );
					assertFalse( Hibernate.isInitialized( e ) );
				}
		);

		scope.inTransaction(
				session -> {
					final EntityMutableNaturalId e = session.get( EntityMutableNaturalId.class, 1 );
					assertEquals( "name", e.name );
				}
		);
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new EntityMutableNaturalId( 1, "name" ) );
					session.persist( new EntityImmutableNaturalId( 1, "name" ) );
				}
		);
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
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

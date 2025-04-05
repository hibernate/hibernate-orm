/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.userdefined;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.EnumSet;
import java.util.UUID;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel(annotatedClasses = {
		BeforeExecutionAssignedValuesTest.EntityWithGeneratedId.class,
		BeforeExecutionAssignedValuesTest.GeneratedCompositeId.class,
		BeforeExecutionAssignedValuesTest.EntityWithGeneratedEmbeddedId.class,
		BeforeExecutionAssignedValuesTest.EntityWithGeneratedProperty.class,
})
@Jira("https://hibernate.atlassian.net/browse/HHH-19320")
class BeforeExecutionAssignedValuesTest {
	@Test
	void testAssignedId(SessionFactoryScope scope) {
		final EntityWithGeneratedId entity1 = new EntityWithGeneratedId( "assigned-id", "assigned-entity" );
		scope.inTransaction( session -> session.persist( entity1 ) );
		assertThat( entity1.getGeneratedId() ).isEqualTo( "assigned-id" );

		final EntityWithGeneratedId entity2 = new EntityWithGeneratedId( "stateless-id", "stateless-entity" );
		scope.inStatelessTransaction( session -> session.insert( entity2 ) );
		assertThat( entity2.getGeneratedId() ).isEqualTo( "stateless-id" );
	}

	@Test
	void testGeneratedId(SessionFactoryScope scope) {
		final EntityWithGeneratedId entity = new EntityWithGeneratedId( null, "assigned-entity" );
		scope.inTransaction( session -> session.persist( entity ) );
		assertThat( entity.getGeneratedId() ).isNotNull();
	}

	@Test
	void testAssignedEmbeddedId(SessionFactoryScope scope) {
		final EntityWithGeneratedEmbeddedId entity1 = new EntityWithGeneratedEmbeddedId(
				new GeneratedCompositeId( "assigned-1", null),
				"generated-entity"
		);
		scope.inTransaction( session -> session.persist( entity1 ) );
		assertThat( entity1.getId().getId1() ).isEqualTo( "assigned-1" );
		assertThat( entity1.getId().getId2() ).isNotNull();

		final EntityWithGeneratedEmbeddedId entity2 = new EntityWithGeneratedEmbeddedId(
				new GeneratedCompositeId( "new-assigned-1", "assigned-2"),
				"generated-entity"
		);
		scope.inTransaction( session -> session.persist( entity2 ) );
		assertThat( entity2.getId().getId1() ).isEqualTo( "new-assigned-1" );
		assertThat( entity2.getId().getId2() ).isEqualTo( "assigned-2" );
	}

	@Test
	void testGeneratedEmbeddedId(SessionFactoryScope scope) {
		final EntityWithGeneratedEmbeddedId entity = new EntityWithGeneratedEmbeddedId(
				new GeneratedCompositeId(),
				"generated-entity"
		);
		scope.inTransaction( session -> session.persist( entity ) );
		assertThat( entity.getId().getId1() ).isNotNull();
		assertThat( entity.getId().getId2() ).isNotNull();
	}

	@Test
	void testInsertAssignedProperty(SessionFactoryScope scope) {
		final String assigned = "assigned-property";
		final EntityWithGeneratedProperty entity = new EntityWithGeneratedProperty( 1L, assigned );
		scope.inTransaction( session -> session.persist( entity ) );
		assertThat( entity.getGeneratedProperty() ).isEqualTo( assigned );
	}

	@Test
	void testGeneratedPropertyAndUpdate(SessionFactoryScope scope) {
		final EntityWithGeneratedProperty entity = new EntityWithGeneratedProperty( 2L, null );
		scope.inTransaction( session -> {
			session.persist( entity );
			session.flush();

			assertThat( entity.getGeneratedProperty() ).isNotNull();

			// test update
			entity.setGeneratedProperty( "new-assigned-property" );
		} );

		assertThat( entity.getGeneratedProperty() ).isEqualTo( "new-assigned-property" );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "EntityWithGeneratedId")
	static class EntityWithGeneratedId {
		@Id
		@GeneratedValue
		@AssignableGenerator
		private String generatedId;

		private String name;

		public EntityWithGeneratedId() {
		}

		public EntityWithGeneratedId(String generatedId, String name) {
			this.generatedId = generatedId;
			this.name = name;
		}

		public String getGeneratedId() {
			return generatedId;
		}

		public String getName() {
			return name;
		}
	}

	@Embeddable
	static class GeneratedCompositeId {
		@AssignableGenerator
		private String id1;

		@AssignableGenerator
		private String id2;

		public GeneratedCompositeId() {
		}

		public GeneratedCompositeId(String id1, String id2) {
			this.id1 = id1;
			this.id2 = id2;
		}

		public String getId1() {
			return id1;
		}

		public String getId2() {
			return id2;
		}
	}

	@Entity(name = "EntityWithGeneratedEmbeddedId")
	static class EntityWithGeneratedEmbeddedId {
		@EmbeddedId
		private GeneratedCompositeId id;

		private String name;

		public EntityWithGeneratedEmbeddedId() {
		}

		public EntityWithGeneratedEmbeddedId(GeneratedCompositeId id, String name) {
			this.id = id;
			this.name = name;
		}

		public GeneratedCompositeId getId() {
			return id;
		}
	}

	@Entity(name = "EntityWithGeneratedProperty")
	static class EntityWithGeneratedProperty {
		@Id
		private Long id;

		@AssignableGenerator
		private String generatedProperty;

		public EntityWithGeneratedProperty() {
		}

		public EntityWithGeneratedProperty(Long id, String generatedProperty) {
			this.id = id;
			this.generatedProperty = generatedProperty;
		}

		public Long getId() {
			return id;
		}

		public String getGeneratedProperty() {
			return generatedProperty;
		}

		public void setGeneratedProperty(String generatedProperty) {
			this.generatedProperty = generatedProperty;
		}
	}

	@IdGeneratorType(AssignedIdGenerator.class)
	@ValueGenerationType(generatedBy = AssignedGenerator.class)
	@Retention(RUNTIME)
	@Target({FIELD, METHOD})
	@interface AssignableGenerator {
	}

	public static class AssignedGenerator implements BeforeExecutionGenerator {
		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			if ( currentValue != null ) {
				return currentValue;
			}
			return UUID.randomUUID().toString();
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_AND_UPDATE;
		}
	}

	public static class AssignedIdGenerator extends AssignedGenerator {
		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}

		@Override
		public boolean allowAssignedIdentifiers() {
			return true;
		}
	}
}

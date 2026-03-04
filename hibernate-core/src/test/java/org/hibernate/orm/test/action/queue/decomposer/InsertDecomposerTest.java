/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.decomposer;

import java.util.List;

import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.InsertDecomposer;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link InsertDecomposer}
 *
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		InsertDecomposerTest.SimpleEntity.class,
		InsertDecomposerTest.EntityWithSecondaryTable.class,
		InsertDecomposerTest.ParentEntity.class,
		InsertDecomposerTest.ChildEntity.class,
		InsertDecomposerTest.DynamicInsertEntity.class,
		InsertDecomposerTest.EntityWithEmbedded.class,
		InsertDecomposerTest.EntityWithAssociation.class,
		InsertDecomposerTest.TargetEntity.class,
		InsertDecomposerTest.EntityWithGeneratedValue.class
})
public class InsertDecomposerTest {

	@Test
	public void testBasicInsertDecomposition(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			SimpleEntity entity = new SimpleEntity();
			entity.name = "Test";

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( SimpleEntity.class );
			InsertDecomposer decomposer = new InsertDecomposer( persister, factory );

			// Create insert action
			EntityInsertAction action = createInsertAction( entity, session, persister );

			// Decompose
			List<PlannedOperationGroup> groups = decomposer.decompose( action, 0, callback -> {}, session );

			// Verify
			assertNotNull( groups );
			assertFalse( groups.isEmpty(), "Should have at least one operation group" );
			assertEquals( 1, groups.size(), "Simple entity should have 1 table" );

			PlannedOperationGroup group = groups.get( 0 );
			assertEquals( MutationKind.INSERT, group.kind() );
			assertFalse( group.operations().isEmpty() );
		} );
	}

	@Test
	public void testSecondaryTableDecomposition(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			EntityWithSecondaryTable entity = new EntityWithSecondaryTable();
			entity.primaryField = "Primary";
			entity.secondaryField = "Secondary";

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( EntityWithSecondaryTable.class );
			InsertDecomposer decomposer = new InsertDecomposer( persister, factory );

			EntityInsertAction action = createInsertAction( entity, session, persister );
			List<PlannedOperationGroup> groups = decomposer.decompose( action, 0, callback -> {}, session );

			// Should have 2 groups (primary table + secondary table)
			assertEquals( 2, groups.size(), "Should have 2 operation groups for secondary table" );

			// Verify all are INSERT operations
			assertTrue( groups.stream().allMatch( g -> g.kind() == MutationKind.INSERT ) );
		} );
	}

	@Test
	public void testJoinedInheritanceDecomposition(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			ChildEntity entity = new ChildEntity();
			entity.parentField = "Parent";
			entity.childField = "Child";

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( ChildEntity.class );
			InsertDecomposer decomposer = new InsertDecomposer( persister, factory );

			EntityInsertAction action = createInsertAction( entity, session, persister );
			List<PlannedOperationGroup> groups = decomposer.decompose( action, 0, callback -> {}, session );

			// Should have 2 groups (parent table + child table)
			assertTrue( groups.size() >= 2, "Joined inheritance should have at least 2 tables" );

			// Verify all are INSERT operations
			assertTrue( groups.stream().allMatch( g -> g.kind() == MutationKind.INSERT ) );
		} );
	}

	@Test
	public void testDynamicInsertDecomposition(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			DynamicInsertEntity entity = new DynamicInsertEntity();
			entity.field1 = "Value1";
			// field2 is null - should not be included in dynamic insert

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( DynamicInsertEntity.class );
			InsertDecomposer decomposer = new InsertDecomposer( persister, factory );

			assertTrue( persister.isDynamicInsert(), "Entity should have dynamic insert enabled" );

			EntityInsertAction action = createInsertAction( entity, session, persister );
			List<PlannedOperationGroup> groups = decomposer.decompose( action, 0, callback -> {}, session );

			assertNotNull( groups );
			assertFalse( groups.isEmpty() );
		} );
	}

	@Test
	public void testEmbeddedComponentDecomposition(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			EntityWithEmbedded entity = new EntityWithEmbedded();
			entity.embedded = new EmbeddableComponent();
			entity.embedded.field1 = "Embedded1";
			entity.embedded.field2 = "Embedded2";

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( EntityWithEmbedded.class );
			InsertDecomposer decomposer = new InsertDecomposer( persister, factory );

			EntityInsertAction action = createInsertAction( entity, session, persister );
			List<PlannedOperationGroup> groups = decomposer.decompose( action, 0, callback -> {}, session );

			assertNotNull( groups );
			assertFalse( groups.isEmpty() );
			// Embedded components should be inserted into the same table
			assertEquals( 1, groups.size() );
		} );
	}

	@Test
	public void testInsertWithAssociation(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// First insert target
			TargetEntity target = new TargetEntity();
			target.targetField = "Target";
			entityManager.persist( target );
			entityManager.flush();

			// Then insert entity with association
			EntityWithAssociation entity = new EntityWithAssociation();
			entity.field = "Field";
			entity.target = target;

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( EntityWithAssociation.class );
			InsertDecomposer decomposer = new InsertDecomposer( persister, factory );

			EntityInsertAction action = createInsertAction( entity, session, persister );
			List<PlannedOperationGroup> groups = decomposer.decompose( action, 0, callback -> {}, session );

			assertNotNull( groups );
			assertFalse( groups.isEmpty() );
		} );
	}

	@Test
	public void testStaticInsertGroup(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( SimpleEntity.class );
			InsertDecomposer decomposer = new InsertDecomposer( persister, factory );

			// Static insert group should be pre-generated
			assertNotNull( decomposer.getStaticMutationGroup() );
			assertTrue( decomposer.getStaticMutationGroup().getNumberOfOperations() > 0 );
		} );
	}

	@Test
	public void testDecompositionWithGeneratedId(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			EntityWithGeneratedValue entity = new EntityWithGeneratedValue();
			entity.name = "Test";

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( EntityWithGeneratedValue.class );
			InsertDecomposer decomposer = new InsertDecomposer( persister, factory );

			EntityInsertAction action = createInsertAction( entity, session, persister );
			List<PlannedOperationGroup> groups = decomposer.decompose( action, 0, callback -> {}, session );

			assertNotNull( groups );
			assertFalse( groups.isEmpty() );
		} );
	}

	@Test
	public void testOrdinalAssignment(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			SimpleEntity entity = new SimpleEntity();
			entity.name = "Test";

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( SimpleEntity.class );
			InsertDecomposer decomposer = new InsertDecomposer( persister, factory );

			EntityInsertAction action = createInsertAction( entity, session, persister );
			int ordinalBase = 5;
			List<PlannedOperationGroup> groups = decomposer.decompose( action, ordinalBase, callback -> {}, session );

			// Verify ordinals are based on the base
			for ( PlannedOperationGroup group : groups ) {
				assertTrue( group.ordinal() >= ordinalBase * 1_000,
						"Ordinal should be >= " + ordinalBase * 1_000 );
			}
		} );
	}

	// Helper method
	private EntityInsertAction createInsertAction(
			Object entity,
			SessionImplementor session,
			EntityPersister persister) {
		Object id = persister.getIdentifier( entity, session );
		Object[] state = persister.getValues( entity );

		return new EntityInsertAction(
				id,
				state,
				entity,
				null, // version
				persister,
				false, // isVersionIncrementDisabled
				(EventSource) session
		);
	}

	// Test entities

	@Entity(name = "SimpleEntity")
	@Table(name = "simple_entity")
	public static class SimpleEntity {
		@Id
		@GeneratedValue
		Long id;

		String name;
	}

	@Entity(name = "EntityWithSecondaryTable")
	@Table(name = "entity_primary")
	@SecondaryTable(name = "entity_secondary")
	public static class EntityWithSecondaryTable {
		@Id
		@GeneratedValue
		Long id;

		String primaryField;

		@jakarta.persistence.Column(table = "entity_secondary")
		String secondaryField;
	}

	@Entity(name = "ParentEntity")
	@Table(name = "ParentEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class ParentEntity {
		@Id
		@GeneratedValue
		Long id;

		String parentField;
	}

	@Entity(name = "ChildEntity")
	@Table(name = "ChildEntity")
	public static class ChildEntity extends ParentEntity {
		String childField;
	}

	@Entity(name = "DynamicInsertEntity")
	@Table(name = "dynamic_insert_entity")
	@DynamicInsert
	public static class DynamicInsertEntity {
		@Id
		@GeneratedValue
		Long id;

		String field1;
		String field2;
	}

	@Embeddable
	public static class EmbeddableComponent {
		String field1;
		String field2;
	}

	@Entity(name = "EntityWithEmbedded")
	@Table(name = "entity_embedded")
	public static class EntityWithEmbedded {
		@Id
		@GeneratedValue
		Long id;

		@Embedded
		EmbeddableComponent embedded;
	}

	@Entity(name = "TargetEntity")
	@Table(name = "target_entity")
	public static class TargetEntity {
		@Id
		@GeneratedValue
		Long id;

		String targetField;
	}

	@Entity(name = "EntityWithAssociation")
	@Table(name = "entity_association")
	public static class EntityWithAssociation {
		@Id
		@GeneratedValue
		Long id;

		String field;

		@ManyToOne
		TargetEntity target;
	}

	@Entity(name = "EntityWithGeneratedValue")
	@Table(name = "entity_generated")
	public static class EntityWithGeneratedValue {
		@Id
		@GeneratedValue
		Long id;

		String name;
	}
}

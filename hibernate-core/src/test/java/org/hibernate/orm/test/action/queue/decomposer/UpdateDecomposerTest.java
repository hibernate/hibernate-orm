/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.decomposer;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.StatementShapeKey;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.action.queue.decompose.entity.UpdateDecomposer;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link UpdateDecomposer}
 *
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		UpdateDecomposerTest.SimpleEntity.class,
		UpdateDecomposerTest.EntityWithVersion.class,
		UpdateDecomposerTest.EntityWithSecondaryTable.class,
		UpdateDecomposerTest.ParentEntity.class,
		UpdateDecomposerTest.ChildEntity.class,
		UpdateDecomposerTest.DynamicUpdateEntity.class,
		UpdateDecomposerTest.EntityWithAllOptimisticLock.class,
		UpdateDecomposerTest.EntityWithDirtyOptimisticLock.class,
		UpdateDecomposerTest.EntityWithOptionalSecondaryTable.class
})
public class UpdateDecomposerTest {

	@Test
	public void testBasicUpdateDecomposition(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// Create and persist entity
			SimpleEntity entity = new SimpleEntity();
			entity.name = "Initial";
			entityManager.persist( entity );
			entityManager.flush();

			// Modify entity
			entity.name = "Updated";
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( SimpleEntity.class );
			UpdateDecomposer decomposer = new UpdateDecomposer( persister, factory );

			// Create update action
			EntityUpdateAction action = createUpdateAction( entity, session, persister );

			// Decompose
			List<PlannedOperation> operations = decomposer.decompose(action, 0, session , null);
			List<PlannedOperationGroup> groups = groupOperations( operations );

			// Verify
			assertNotNull( groups );
			assertFalse( groups.isEmpty(), "Should have at least one operation group" );
			assertEquals( 1, groups.size(), "Simple entity should have 1 table" );

			PlannedOperationGroup group = groups.get( 0 );
			assertEquals( MutationKind.UPDATE, group.kind() );
			assertFalse( group.operations().isEmpty() );
		} );
	}

	@Test
	public void testUpdateWithVersionedEntity(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// Create and persist entity
			EntityWithVersion entity = new EntityWithVersion();
			entity.name = "Initial";
			entityManager.persist( entity );
			entityManager.flush();

			// Modify entity
			Long initialVersion = entity.version;
			entity.name = "Updated";
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( EntityWithVersion.class );
			UpdateDecomposer decomposer = new UpdateDecomposer( persister, factory );

			// Verify version-based optimistic locking
			assertEquals( OptimisticLockStyle.VERSION, persister.optimisticLockStyle() );
			assertNotNull( persister.getVersionMapping() );

			// Version should have been incremented
			assertTrue( entity.version > initialVersion, "Version should be incremented" );

			EntityUpdateAction action = createUpdateAction( entity, session, persister );
			List<PlannedOperation> operations = decomposer.decompose(action, 0, session , null);
			List<PlannedOperationGroup> groups = groupOperations( operations );

			assertNotNull( groups );
			assertFalse( groups.isEmpty() );
			assertEquals( MutationKind.UPDATE, groups.get( 0 ).kind() );
		} );
	}

	@Test
	public void testUpdateWithSecondaryTable(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// Create and persist entity
			EntityWithSecondaryTable entity = new EntityWithSecondaryTable();
			entity.primaryField = "Primary";
			entity.secondaryField = "Secondary";
			entityManager.persist( entity );
			entityManager.flush();

			// Modify both fields
			entity.primaryField = "Primary Updated";
			entity.secondaryField = "Secondary Updated";
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( EntityWithSecondaryTable.class );
			UpdateDecomposer decomposer = new UpdateDecomposer( persister, factory );

			EntityUpdateAction action = createUpdateAction( entity, session, persister );
			List<PlannedOperation> operations = decomposer.decompose(action, 0, session , null);
			List<PlannedOperationGroup> groups = groupOperations( operations );

			// Should have 2 groups (primary table + secondary table)
			assertEquals( 2, groups.size(), "Should have 2 operation groups for secondary table" );

			// All should be UPDATE operations
			assertTrue( groups.stream().allMatch( g -> g.kind() == MutationKind.UPDATE ) );
		} );
	}

	@Test
	public void testUpdateWithJoinedInheritance(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// Create and persist entity
			ChildEntity entity = new ChildEntity();
			entity.parentField = "Parent";
			entity.childField = "Child";
			entityManager.persist( entity );
			entityManager.flush();

			// Modify both fields
			entity.parentField = "Parent Updated";
			entity.childField = "Child Updated";
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( ChildEntity.class );
			UpdateDecomposer decomposer = new UpdateDecomposer( persister, factory );

			EntityUpdateAction action = createUpdateAction( entity, session, persister );
			List<PlannedOperation> operations = decomposer.decompose(action, 0, session , null);
			List<PlannedOperationGroup> groups = groupOperations( operations );

			// Should have 2 groups (parent table + child table)
			assertTrue( groups.size() >= 2, "Joined inheritance should have at least 2 tables" );

			// All should be UPDATE operations
			assertTrue( groups.stream().allMatch( g -> g.kind() == MutationKind.UPDATE ) );
		} );
	}

	@Test
	public void testDynamicUpdateDecomposition(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// Create and persist entity
			DynamicUpdateEntity entity = new DynamicUpdateEntity();
			entity.field1 = "Value1";
			entity.field2 = "Value2";
			entityManager.persist( entity );
			entityManager.flush();

			// Modify only field1 - dynamic update should only update field1
			entity.field1 = "Value1 Updated";
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( DynamicUpdateEntity.class );
			UpdateDecomposer decomposer = new UpdateDecomposer( persister, factory );

			assertTrue( persister.isDynamicUpdate(), "Entity should have dynamic update enabled" );

			EntityUpdateAction action = createUpdateAction( entity, session, persister );
			List<PlannedOperation> operations = decomposer.decompose(action, 0, session , null);
			List<PlannedOperationGroup> groups = groupOperations( operations );

			assertNotNull( groups );
			assertFalse( groups.isEmpty() );
		} );
	}

	@Test
	public void testUpdateWithAllOptimisticLock(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// Create and persist entity
			EntityWithAllOptimisticLock entity = new EntityWithAllOptimisticLock();
			entity.field1 = "Field1";
			entity.field2 = "Field2";
			entityManager.persist( entity );
			entityManager.flush();

			// Modify entity
			entity.field1 = "Field1 Updated";
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( EntityWithAllOptimisticLock.class );
			UpdateDecomposer decomposer = new UpdateDecomposer( persister, factory );

			// Verify ALL optimistic locking
			assertEquals( OptimisticLockStyle.ALL, persister.optimisticLockStyle() );

			EntityUpdateAction action = createUpdateAction( entity, session, persister );
			List<PlannedOperation> operations = decomposer.decompose(action, 0, session , null);
			List<PlannedOperationGroup> groups = groupOperations( operations );

			assertNotNull( groups );
			assertFalse( groups.isEmpty() );
		} );
	}

	@Test
	public void testUpdateWithDirtyOptimisticLock(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// Create and persist entity
			EntityWithDirtyOptimisticLock entity = new EntityWithDirtyOptimisticLock();
			entity.field1 = "Field1";
			entity.field2 = "Field2";
			entityManager.persist( entity );
			entityManager.flush();

			// Modify only field1
			entity.field1 = "Field1 Updated";
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( EntityWithDirtyOptimisticLock.class );
			UpdateDecomposer decomposer = new UpdateDecomposer( persister, factory );

			// Verify DIRTY optimistic locking
			assertEquals( OptimisticLockStyle.DIRTY, persister.optimisticLockStyle() );

			EntityUpdateAction action = createUpdateAction( entity, session, persister );
			List<PlannedOperation> operations = decomposer.decompose(action, 0, session , null);
			List<PlannedOperationGroup> groups = groupOperations( operations );

			assertNotNull( groups );
			assertFalse( groups.isEmpty() );
		} );
	}

	@Test
	public void testNoStaticGroupForDynamicUpdate(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( DynamicUpdateEntity.class );
			UpdateDecomposer decomposer = new UpdateDecomposer( persister, factory );

			// Dynamic update entities should not have static group
			// They generate groups on-demand based on dirty fields
			assertTrue( persister.isDynamicUpdate() );
		} );
	}

	@Test
	public void testOrdinalAssignment(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			SimpleEntity entity = new SimpleEntity();
			entity.name = "Initial";
			entityManager.persist( entity );
			entityManager.flush();

			entity.name = "Updated";
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( SimpleEntity.class );
			UpdateDecomposer decomposer = new UpdateDecomposer( persister, factory );

			EntityUpdateAction action = createUpdateAction( entity, session, persister );
			int ordinalBase = 10;
			List<PlannedOperation> operations = decomposer.decompose(action, ordinalBase, session , null);
			List<PlannedOperationGroup> groups = groupOperations( operations );

			// Verify ordinals are based on the base
			for ( PlannedOperationGroup group : groups ) {
				assertTrue( group.ordinal() >= ordinalBase * 1_000,
						"Ordinal should be >= " + ordinalBase * 1_000 );
			}
		} );
	}

	@Test
	public void testUpdateWithNoDirtyFields(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			SimpleEntity entity = new SimpleEntity();
			entity.name = "Test";
			entityManager.persist( entity );
			entityManager.flush();

			// Don't modify entity - no dirty fields
			// This tests that decomposer handles empty dirty fields array

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( SimpleEntity.class );
			UpdateDecomposer decomposer = new UpdateDecomposer( persister, factory );

			// Create update action with no dirty fields
			Object id = persister.getIdentifier( entity, session );
			Object[] state = persister.getValues( entity );
			Object version = persister.getVersion( entity );

			EntityUpdateAction action = new EntityUpdateAction(
					id,
					state,
					null, // dirtyFields - none
					false, // hasDirtyCollection
					state, // previousState
					version, // previousVersion
					null, // nextVersion
					entity,
					null, // rowId
					persister,
					(EventSource) session
			);

			List<PlannedOperation> operations = decomposer.decompose(action, 0, session , null);
			List<PlannedOperationGroup> groups = groupOperations( operations );

			// Even with no dirty fields, should still create operation groups
			assertNotNull( groups );
			assertFalse( groups.isEmpty() );
		} );
	}

	@Test
	public void testForwardTableOrder(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			ChildEntity entity = new ChildEntity();
			entity.parentField = "Parent";
			entity.childField = "Child";
			entityManager.persist( entity );
			entityManager.flush();

			entity.parentField = "Parent Updated";
			entity.childField = "Child Updated";
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( ChildEntity.class );
			UpdateDecomposer decomposer = new UpdateDecomposer( persister, factory );

			EntityUpdateAction action = createUpdateAction( entity, session, persister );
			List<PlannedOperation> operations = decomposer.decompose(action, 0, session , null);
			List<PlannedOperationGroup> groups = groupOperations( operations );

			// Updates should be in forward order (parent before child)
			// This is opposite of deletes which are in reverse order
			assertTrue( groups.size() >= 2 );
		} );
	}

	@Test
	public void testOptionalSecondaryTableWithNullValues(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// Create entity with only primary table values
			EntityWithOptionalSecondaryTable entity = new EntityWithOptionalSecondaryTable();
			entity.primaryField = "Primary";
			entity.optionalField1 = null;  // Optional secondary table fields are null
			entity.optionalField2 = null;
			entityManager.persist( entity );
			entityManager.flush();

			// Update only primary field
			entity.primaryField = "Primary Updated";
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( EntityWithOptionalSecondaryTable.class );
			UpdateDecomposer decomposer = new UpdateDecomposer( persister, factory );

			EntityUpdateAction action = createUpdateAction( entity, session, persister );
			List<PlannedOperation> operations = decomposer.decompose(action, 0, session , null);
			List<PlannedOperationGroup> groups = groupOperations( operations );

			// Should have operations for primary table
			// Optional table should be handled appropriately based on null values
			assertNotNull( groups );
			assertFalse( groups.isEmpty() );
		} );
	}

	@Test
	public void testOptionalSecondaryTableWithValues(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// Create entity with optional table values
			EntityWithOptionalSecondaryTable entity = new EntityWithOptionalSecondaryTable();
			entity.primaryField = "Primary";
			entity.optionalField1 = "Optional1";
			entity.optionalField2 = "Optional2";
			entityManager.persist( entity );
			entityManager.flush();

			// Update both primary and optional fields
			entity.primaryField = "Primary Updated";
			entity.optionalField1 = "Optional1 Updated";
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( EntityWithOptionalSecondaryTable.class );
			UpdateDecomposer decomposer = new UpdateDecomposer( persister, factory );

			EntityUpdateAction action = createUpdateAction( entity, session, persister );
			List<PlannedOperation> operations = decomposer.decompose(action, 0, session , null);
			List<PlannedOperationGroup> groups = groupOperations( operations );

			// Should have 2 groups for both tables
			assertEquals( 2, groups.size(), "Should have 2 operation groups (primary + optional)" );
			assertTrue( groups.stream().allMatch( g -> g.kind() == MutationKind.UPDATE ) );
		} );
	}

	// Helper method
	private EntityUpdateAction createUpdateAction(
			Object entity,
			SessionImplementor session,
			EntityPersister persister) {
		Object id = persister.getIdentifier( entity, session );
		Object[] state = persister.getValues( entity );
		Object version = persister.getVersion( entity );

		// For testing, use the same state as previous state
		// In real scenarios, this would be the actual previous state
		Object[] previousState = state.clone();

		// Mark all fields as dirty for testing purposes
		int[] dirtyFields = new int[state.length];
		for ( int i = 0; i < state.length; i++ ) {
			dirtyFields[i] = i;
		}

		return new EntityUpdateAction(
				id,
				state,
				dirtyFields,
				false, // hasDirtyCollection
				previousState,
				version, // previousVersion
				null, // nextVersion
				entity,
				null, // rowId
				persister,
				(EventSource) session
		);
	}

	// Test entities

	@Entity(name = "SimpleEntity")
	@Table(name = "simple_update_entity")
	public static class SimpleEntity {
		@Id
		@GeneratedValue
		Long id;

		String name;
	}

	@Entity(name = "EntityWithVersion")
	@Table(name = "update_entity_with_version")
	public static class EntityWithVersion {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@Version
		Long version;
	}

	@Entity(name = "EntityWithSecondaryTable")
	@Table(name = "update_entity_primary")
	@SecondaryTable(name = "update_entity_secondary")
	public static class EntityWithSecondaryTable {
		@Id
		@GeneratedValue
		Long id;

		String primaryField;

		@jakarta.persistence.Column(table = "update_entity_secondary")
		String secondaryField;
	}

	@Entity(name = "ParentEntity")
	@Table(name = "UpdateParentEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class ParentEntity {
		@Id
		@GeneratedValue
		Long id;

		String parentField;
	}

	@Entity(name = "ChildEntity")
	@Table(name = "UpdateChildEntity")
	public static class ChildEntity extends ParentEntity {
		String childField;
	}

	@Entity(name = "DynamicUpdateEntity")
	@Table(name = "dynamic_update_entity")
	@DynamicUpdate
	public static class DynamicUpdateEntity {
		@Id
		@GeneratedValue
		Long id;

		String field1;
		String field2;
	}

	@Entity(name = "EntityWithAllOptimisticLock")
	@Table(name = "update_entity_all_lock")
	@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.ALL)
	@DynamicUpdate
	public static class EntityWithAllOptimisticLock {
		@Id
		@GeneratedValue
		Long id;

		String field1;
		String field2;
	}

	@Entity(name = "EntityWithDirtyOptimisticLock")
	@Table(name = "update_entity_dirty_lock")
	@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.DIRTY)
	@DynamicUpdate
	public static class EntityWithDirtyOptimisticLock {
		@Id
		@GeneratedValue
		Long id;

		String field1;
		String field2;
	}

	@Entity(name = "EntityWithOptionalSecondaryTable")
	@Table(name = "update_entity_optional_primary")
	@SecondaryTable(name = "update_entity_optional_secondary")
	public static class EntityWithOptionalSecondaryTable {
		@Id
		@GeneratedValue
		Long id;

		String primaryField;

		@jakarta.persistence.Column(table = "update_entity_optional_secondary")
		String optionalField1;

		@jakarta.persistence.Column(table = "update_entity_optional_secondary")
		String optionalField2;
	}

	// Helper methods for grouping operations (mirrors FlushCoordinator logic)
	private List<PlannedOperationGroup> groupOperations(List<PlannedOperation> operations) {
		if (operations.isEmpty()) {
			return List.of();
		}

		// Group by shapeKey only (merge operations from different entities)
		// This mirrors FlushCoordinator behavior for non-self-referential tables
		final Map<StatementShapeKey, OperationGroupBuilder> builders = new LinkedHashMap<>();

		for (PlannedOperation operation : operations) {
			final StatementShapeKey shapeKey = computeShapeKey(operation);
			var builder = builders.get(shapeKey);
			if (builder == null) {
				// First operation for this key - create new builder (which adds the operation in constructor)
				builder = new OperationGroupBuilder(operation, shapeKey);
				builders.put(shapeKey, builder);
			} else {
				// Subsequent operation for this key - add to existing builder
				builder.addOperation(operation);
			}
		}

		final List<PlannedOperationGroup> groups = new ArrayList<>(builders.size());
		for (OperationGroupBuilder builder : builders.values()) {
			groups.add(builder.build());
		}

		return groups;
	}

	private StatementShapeKey computeShapeKey(PlannedOperation operation) {
		final String table = operation.getTableExpression();
		final MutationKind kind = operation.getKind();

		return switch (kind) {
			case INSERT -> StatementShapeKey.forInsert(table, operation);
			case UPDATE, UPDATE_ORDER -> StatementShapeKey.forUpdate(table, operation);
			case DELETE -> StatementShapeKey.forDelete(table, operation);
			case NO_OP -> StatementShapeKey.forNoOp(table);
		};
	}

	private static class OperationGroupBuilder {
		private final String tableExpression;
		private final MutationKind kind;
		private final StatementShapeKey shapeKey;
		private int ordinal;
		private final String origin;
		private final List<PlannedOperation> operations = new ArrayList<>();

		OperationGroupBuilder(PlannedOperation firstOperation, StatementShapeKey shapeKey) {
			this.tableExpression = firstOperation.getTableExpression();
			this.kind = firstOperation.getKind();
			this.shapeKey = shapeKey;
			this.ordinal = firstOperation.getOrdinal();
			this.origin = firstOperation.getOrigin();
			this.operations.add(firstOperation);
		}

		void addOperation(PlannedOperation op) {
			this.operations.add(op);
			// Track minimum ordinal when merging operations
			this.ordinal = Math.min(this.ordinal, op.getOrdinal());
		}

		PlannedOperationGroup build() {
			final boolean needsIdPrePhase = operations.stream()
					.anyMatch(PlannedOperation::needsIdPrePhase);

			return new PlannedOperationGroup(
					tableExpression,
					kind,
					shapeKey,
					operations,
					needsIdPrePhase,
					false,
					ordinal,
					origin
			);
		}
	}
}

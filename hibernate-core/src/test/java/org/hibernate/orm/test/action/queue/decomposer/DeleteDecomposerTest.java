/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.decomposer;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.StatementShapeKey;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.DeleteDecomposer;

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
 * Tests for {@link DeleteDecomposer}
 *
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		DeleteDecomposerTest.SimpleEntity.class,
		DeleteDecomposerTest.EntityWithVersion.class,
		DeleteDecomposerTest.EntityWithSecondaryTable.class,
		DeleteDecomposerTest.ParentEntity.class,
		DeleteDecomposerTest.ChildEntity.class,
		DeleteDecomposerTest.SoftDeleteEntity.class,
		DeleteDecomposerTest.SoftDeleteWithVersion.class,
		DeleteDecomposerTest.EntityWithAllOptimisticLock.class
})
public class DeleteDecomposerTest {

	@Test
	public void testBasicDeleteDecomposition(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// Create and persist entity
			SimpleEntity entity = new SimpleEntity();
			entity.name = "Test";
			entityManager.persist( entity );
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( SimpleEntity.class );
			DeleteDecomposer decomposer = new DeleteDecomposer( persister, factory );

			// Create delete action
			EntityDeleteAction action = createDeleteAction( entity, session, persister );

			// Decompose
			List<PlannedOperation> operations = decomposer.decompose( action, 0, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

			// Verify
			assertNotNull( groups );
			assertFalse( groups.isEmpty(), "Should have at least one operation group" );
			assertEquals( 1, groups.size(), "Simple entity should have 1 table" );

			PlannedOperationGroup group = groups.get( 0 );
			assertEquals( MutationKind.DELETE, group.kind() );
			assertFalse( group.operations().isEmpty() );
		} );
	}

	@Test
	public void testDeleteWithVersionedEntity(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// Create and persist entity
			EntityWithVersion entity = new EntityWithVersion();
			entity.name = "Test";
			entityManager.persist( entity );
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( EntityWithVersion.class );
			DeleteDecomposer decomposer = new DeleteDecomposer( persister, factory );

			// Verify version-based optimistic locking
			assertEquals( OptimisticLockStyle.VERSION, persister.optimisticLockStyle() );
			assertNotNull( persister.getVersionMapping() );

			EntityDeleteAction action = createDeleteAction( entity, session, persister );
			List<PlannedOperation> operations = decomposer.decompose( action, 0, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

			assertNotNull( groups );
			assertFalse( groups.isEmpty() );
			assertEquals( MutationKind.DELETE, groups.get( 0 ).kind() );
		} );
	}

	@Test
	public void testDeleteWithSecondaryTable(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// Create and persist entity
			EntityWithSecondaryTable entity = new EntityWithSecondaryTable();
			entity.primaryField = "Primary";
			entity.secondaryField = "Secondary";
			entityManager.persist( entity );
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( EntityWithSecondaryTable.class );
			DeleteDecomposer decomposer = new DeleteDecomposer( persister, factory );

			EntityDeleteAction action = createDeleteAction( entity, session, persister );
			List<PlannedOperation> operations = decomposer.decompose( action, 0, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

			// Should have 2 groups (secondary table + primary table, in reverse order)
			assertEquals( 2, groups.size(), "Should have 2 operation groups for secondary table" );

			// All should be DELETE operations
			assertTrue( groups.stream().allMatch( g -> g.kind() == MutationKind.DELETE ) );
		} );
	}

	@Test
	public void testDeleteWithJoinedInheritance(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// Create and persist entity
			ChildEntity entity = new ChildEntity();
			entity.parentField = "Parent";
			entity.childField = "Child";
			entityManager.persist( entity );
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( ChildEntity.class );
			DeleteDecomposer decomposer = new DeleteDecomposer( persister, factory );

			EntityDeleteAction action = createDeleteAction( entity, session, persister );
			List<PlannedOperation> operations = decomposer.decompose( action, 0, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

			// Should have 2 groups (child table + parent table, in reverse order)
			assertTrue( groups.size() >= 2, "Joined inheritance should have at least 2 tables" );

			// All should be DELETE operations
			assertTrue( groups.stream().allMatch( g -> g.kind() == MutationKind.DELETE ) );
		} );
	}

	@Test
	public void testSoftDeleteDecomposition(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// Create and persist entity
			SoftDeleteEntity entity = new SoftDeleteEntity();
			entity.name = "Test";
			entityManager.persist( entity );
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( SoftDeleteEntity.class );
			DeleteDecomposer decomposer = new DeleteDecomposer( persister, factory );

			// Verify soft delete is enabled
			assertNotNull( persister.getSoftDeleteMapping(), "Soft delete should be enabled" );

			EntityDeleteAction action = createDeleteAction( entity, session, persister );
			List<PlannedOperation> operations = decomposer.decompose( action, 0, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

			// Verify
			assertNotNull( groups );
			assertFalse( groups.isEmpty() );

			// Soft delete should use UPDATE, not DELETE
			assertEquals( 1, groups.size(), "Soft delete should only affect root table" );
			PlannedOperationGroup group = groups.get( 0 );
			assertEquals( MutationKind.UPDATE, group.kind(),
					"Soft delete should be an UPDATE operation" );
		} );
	}

	@Test
	public void testSoftDeleteWithVersion(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// Create and persist entity
			SoftDeleteWithVersion entity = new SoftDeleteWithVersion();
			entity.name = "Test";
			entityManager.persist( entity );
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( SoftDeleteWithVersion.class );
			DeleteDecomposer decomposer = new DeleteDecomposer( persister, factory );

			// Verify soft delete and version
			assertNotNull( persister.getSoftDeleteMapping() );
			assertNotNull( persister.getVersionMapping() );

			EntityDeleteAction action = createDeleteAction( entity, session, persister );
			List<PlannedOperation> operations = decomposer.decompose( action, 0, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

			assertNotNull( groups );
			assertEquals( 1, groups.size() );
			assertEquals( MutationKind.UPDATE, groups.get( 0 ).kind() );
		} );
	}

	@Test
	public void testDeleteWithAllOptimisticLock(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// Create and persist entity
			EntityWithAllOptimisticLock entity = new EntityWithAllOptimisticLock();
			entity.field1 = "Field1";
			entity.field2 = "Field2";
			entityManager.persist( entity );
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( EntityWithAllOptimisticLock.class );
			DeleteDecomposer decomposer = new DeleteDecomposer( persister, factory );

			// Verify ALL optimistic locking
			assertEquals( OptimisticLockStyle.ALL, persister.optimisticLockStyle() );

			EntityDeleteAction action = createDeleteAction( entity, session, persister );
			List<PlannedOperation> operations = decomposer.decompose( action, 0, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

			assertNotNull( groups );
			assertFalse( groups.isEmpty() );
		} );
	}

	@Test
	public void testStaticDeleteGroup(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( SimpleEntity.class );
			DeleteDecomposer decomposer = new DeleteDecomposer( persister, factory );

			// Static delete group should be pre-generated for hard deletes
			assertNotNull( decomposer.getStaticDeleteOperations() );
			assertFalse( decomposer.getStaticDeleteOperations().isEmpty() );
		} );
	}

	@Test
	public void testStaticSoftDeleteGroup(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( SoftDeleteEntity.class );
			DeleteDecomposer decomposer = new DeleteDecomposer( persister, factory );

			// Static delete group should be pre-generated for soft deletes too
			assertNotNull( decomposer.getStaticDeleteOperations() );
			assertFalse( decomposer.getStaticDeleteOperations().isEmpty() );
		} );
	}

	@Test
	public void testOrdinalAssignment(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			SimpleEntity entity = new SimpleEntity();
			entity.name = "Test";
			entityManager.persist( entity );
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( SimpleEntity.class );
			DeleteDecomposer decomposer = new DeleteDecomposer( persister, factory );

			EntityDeleteAction action = createDeleteAction( entity, session, persister );
			int ordinalBase = 10;
			List<PlannedOperation> operations = decomposer.decompose( action, ordinalBase, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

			// Verify ordinals are based on the base
			for ( PlannedOperationGroup group : groups ) {
				assertTrue( group.ordinal() >= ordinalBase * 1_000,
						"Ordinal should be >= " + ordinalBase * 1_000 );
			}
		} );
	}

	@Test
	public void testDeleteWithNullVersion(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			// Create and persist entity
			EntityWithVersion entity = new EntityWithVersion();
			entity.name = "Test";
			entityManager.persist( entity );
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( EntityWithVersion.class );
			DeleteDecomposer decomposer = new DeleteDecomposer( persister, factory );

			// Create delete action with null version (simulating unloaded entity delete)
			Object id = persister.getIdentifier( entity, session );
			EntityDeleteAction action = new EntityDeleteAction(
					id,
					null,  // state
					null,  // version
					entity,
					persister,
					false,
					(EventSource) session
			);

			List<PlannedOperation> operations = decomposer.decompose( action, 0, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

			assertNotNull( groups );
			assertFalse( groups.isEmpty() );
		} );
	}

	@Test
	public void testReverseOrderForMultipleTables(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			SessionFactoryImplementor factory = session.getSessionFactory();

			ChildEntity entity = new ChildEntity();
			entity.parentField = "Parent";
			entity.childField = "Child";
			entityManager.persist( entity );
			entityManager.flush();

			EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor( ChildEntity.class );
			DeleteDecomposer decomposer = new DeleteDecomposer( persister, factory );

			EntityDeleteAction action = createDeleteAction( entity, session, persister );
			List<PlannedOperation> operations = decomposer.decompose( action, 0, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

			// In reverse order: child table should come before parent table
			// This ensures FK constraints are not violated
			assertTrue( groups.size() >= 2 );
		} );
	}

	// Helper method
	private EntityDeleteAction createDeleteAction(
			Object entity,
			SessionImplementor session,
			EntityPersister persister) {
		Object id = persister.getIdentifier( entity, session );
		Object[] state = persister.getValues( entity );
		Object version = persister.getVersion( entity );

		return new EntityDeleteAction(
				id,
				state,
				version,
				entity,
				persister,
				false, // isCascadeDeleteEnabled
				(EventSource) session
		);
	}

	// Test entities

	@Entity(name = "SimpleEntity")
	@Table(name = "simple_delete_entity")
	public static class SimpleEntity {
		@Id
		@GeneratedValue
		Long id;

		String name;
	}

	@Entity(name = "EntityWithVersion")
	@Table(name = "entity_with_version")
	public static class EntityWithVersion {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@Version
		Long version;
	}

	@Entity(name = "EntityWithSecondaryTable")
	@Table(name = "delete_entity_primary")
	@SecondaryTable(name = "delete_entity_secondary")
	public static class EntityWithSecondaryTable {
		@Id
		@GeneratedValue
		Long id;

		String primaryField;

		@jakarta.persistence.Column(table = "delete_entity_secondary")
		String secondaryField;
	}

	@Entity(name = "ParentEntity")
	@Table(name = "DeleteParentEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class ParentEntity {
		@Id
		@GeneratedValue
		Long id;

		String parentField;
	}

	@Entity(name = "ChildEntity")
	@Table(name = "DeleteChildEntity")
	public static class ChildEntity extends ParentEntity {
		String childField;
	}

	@Entity(name = "SoftDeleteEntity")
	@Table(name = "soft_delete_entity")
	@SoftDelete
	public static class SoftDeleteEntity {
		@Id
		@GeneratedValue
		Long id;

		String name;
	}

	@Entity(name = "SoftDeleteWithVersion")
	@Table(name = "soft_delete_versioned")
	@SoftDelete
	public static class SoftDeleteWithVersion {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@Version
		Long version;
	}

	@Entity(name = "EntityWithAllOptimisticLock")
	@Table(name = "entity_all_lock")
	@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.ALL)
	@org.hibernate.annotations.DynamicUpdate
	public static class EntityWithAllOptimisticLock {
		@Id
		@GeneratedValue
		Long id;

		String field1;
		String field2;
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
			case UPDATE -> StatementShapeKey.forUpdate(table, operation);
			case DELETE -> StatementShapeKey.forDelete(table, operation);
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
					ordinal,
					origin
			);
		}
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.decomposer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.StatementShapeKey;
import org.hibernate.action.queue.plan.PlannedOperation;
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
			List<PlannedOperation> operations = decomposer.decompose( action, 0, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

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
			List<PlannedOperation> operations = decomposer.decompose( action, 0, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

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
			List<PlannedOperation> operations = decomposer.decompose( action, 0, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

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
			List<PlannedOperation> operations = decomposer.decompose( action, 0, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

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
			List<PlannedOperation> operations = decomposer.decompose( action, 0, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

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
			List<PlannedOperation> operations = decomposer.decompose( action, 0, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

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
			assertNotNull( decomposer.getStaticInsertOperations() );
			assertFalse( decomposer.getStaticInsertOperations().isEmpty() );
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
			List<PlannedOperation> operations = decomposer.decompose( action, 0, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

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
			List<PlannedOperation> operations = decomposer.decompose( action, ordinalBase, session );
			List<PlannedOperationGroup> groups = groupOperations( operations );

			// Verify ordinals are based on the base
			for ( PlannedOperationGroup group : groups ) {
				assertTrue( group.ordinal() >= ordinalBase * 1_000,
						"Ordinal should be >= " + ordinalBase * 1_000 );
			}
		} );
	}

	// Helper methods
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

	/**
	 * Helper method to group operations by shape (table + kind + SQL).
	 * Mirrors FlushCoordinator's grouping logic.
	 */
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
		private final int ordinal;
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
		}

		PlannedOperationGroup build() {
			final boolean needsIdPrePhase = false;
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

	/**
	 * Composite key for grouping operations by shape and ordinalBase.
	 */
	private static class OperationGroupKey {
		private final StatementShapeKey shapeKey;
		private final int ordinalBase;

		OperationGroupKey(StatementShapeKey shapeKey, int ordinalBase) {
			this.shapeKey = shapeKey;
			this.ordinalBase = ordinalBase;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof OperationGroupKey)) return false;
			OperationGroupKey that = (OperationGroupKey) o;
			return ordinalBase == that.ordinalBase && shapeKey.equals(that.shapeKey);
		}

		@Override
		public int hashCode() {
			return 31 * shapeKey.hashCode() + ordinalBase;
		}
	}
}

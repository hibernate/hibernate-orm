/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.decomposer;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.QueueType;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.cfg.FlushSettings;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests graph decomposition for temporal history-table mutations.
 *
 * @author Steve Ebersole
 */
@Jpa(
		annotatedClasses = {
				HistoryTableDecomposerTest.HistoryEntity.class,
				HistoryTableDecomposerTest.HistorySecondaryEntity.class,
				HistoryTableDecomposerTest.HistoryJoinedParent.class,
				HistoryTableDecomposerTest.HistoryJoinedChild.class
		},
		properties = {
				@Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "graph"),
				@Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE")
		}
)
public class HistoryTableDecomposerTest {

	@Test
	public void testHistoryInsertAddsHistoryInsert(EntityManagerFactoryScope scope) {
		final var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort( "Skipping GRAPH test with non-GRAPH queue type" );
		}

		scope.inTransaction( entityManager -> {
			final SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			final EntityPersister persister = sfi.getMappingMetamodel().getEntityDescriptor( HistoryEntity.class );
			final HistoryEntity entity = new HistoryEntity();
			entity.id = 1L;
			entity.name = "created";

			final EntityInsertAction action = new EntityInsertAction(
					entity.id,
					persister.getValues( entity ),
					entity,
					persister.getVersion( entity ),
					persister,
					false,
					(EventSource) session
			);
			final List<FlushOperation> operations = new ArrayList<>();
			persister.getInsertDecomposer().decompose( action, 0, session, null, operations::add );

			assertEquals( 2, operations.size() );
			assertEquals( MutationKind.INSERT, operations.get( 0 ).getKind() );
			assertEquals( MutationKind.INSERT, operations.get( 1 ).getKind() );
		} );
	}

	@Test
	public void testHistoryUpdateAddsHistoryEndAndInsert(EntityManagerFactoryScope scope) {
		final var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort( "Skipping GRAPH test with non-GRAPH queue type" );
		}

		scope.inTransaction( entityManager -> {
			final SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			final HistoryEntity entity = new HistoryEntity();
			entity.id = 2L;
			entity.name = "before";
			entityManager.persist( entity );
			entityManager.flush();

			final EntityPersister persister = sfi.getMappingMetamodel().getEntityDescriptor( HistoryEntity.class );
			final Object[] previousState = persister.getValues( entity );
			entity.name = "after";

			final EntityUpdateAction action = new EntityUpdateAction(
					persister.getIdentifier( entity, session ),
					persister.getValues( entity ),
					new int[] { 0 },
					false,
					previousState,
					persister.getVersion( entity ),
					null,
					entity,
					null,
					persister,
					(EventSource) session
			);
			final List<FlushOperation> operations = new ArrayList<>();
			persister.getUpdateDecomposer().decompose( action, 0, session, null, operations::add );

			assertEquals( 3, operations.size() );
			assertEquals( MutationKind.UPDATE, operations.get( 0 ).getKind() );
			assertEquals( MutationKind.UPDATE, operations.get( 1 ).getKind() );
			assertEquals( MutationKind.INSERT, operations.get( 2 ).getKind() );

			entity.name = "before";
		} );
	}

	@Test
	public void testHistoryDeleteAddsHistoryEnd(EntityManagerFactoryScope scope) {
		final var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort( "Skipping GRAPH test with non-GRAPH queue type" );
		}

		scope.inTransaction( entityManager -> {
			final SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			final HistoryEntity entity = new HistoryEntity();
			entity.id = 3L;
			entity.name = "deleted";
			entityManager.persist( entity );
			entityManager.flush();

			final EntityPersister persister = sfi.getMappingMetamodel().getEntityDescriptor( HistoryEntity.class );
			final EntityDeleteAction action = new EntityDeleteAction(
					persister.getIdentifier( entity, session ),
					persister.getValues( entity ),
					persister.getVersion( entity ),
					entity,
					persister,
					false,
					(EventSource) session
			);
			final List<FlushOperation> operations = new ArrayList<>();
			persister.getDeleteDecomposer().decompose( action, 0, session, null, operations::add );

			assertEquals( 2, operations.size() );
			assertEquals( MutationKind.DELETE, operations.get( 0 ).getKind() );
			assertEquals( MutationKind.UPDATE, operations.get( 1 ).getKind() );
		} );
	}

	@Test
	public void testSecondaryTableHistoryInsertAddsSingleHistoryInsert(EntityManagerFactoryScope scope) {
		final var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort( "Skipping GRAPH test with non-GRAPH queue type" );
		}

		scope.inTransaction( entityManager -> {
			final SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			final EntityPersister persister = sfi.getMappingMetamodel().getEntityDescriptor( HistorySecondaryEntity.class );
			final HistorySecondaryEntity entity = new HistorySecondaryEntity();
			entity.id = 4L;
			entity.primaryName = "primary";
			entity.secondaryName = "secondary";

			final EntityInsertAction action = new EntityInsertAction(
					entity.id,
					persister.getValues( entity ),
					entity,
					persister.getVersion( entity ),
					persister,
					false,
					(EventSource) session
			);
			final List<FlushOperation> operations = new ArrayList<>();
			persister.getInsertDecomposer().decompose( action, 0, session, null, operations::add );

			assertEquals( 3, operations.size() );
			assertOperation( operations.get( 0 ), MutationKind.INSERT, "graph_history_secondary_entity" );
			assertOperation( operations.get( 1 ), MutationKind.INSERT, "graph_history_secondary_entity_details" );
			assertOperation( operations.get( 2 ), MutationKind.INSERT, "graph_history_secondary_entity_history" );
		} );
	}

	@Test
	public void testSecondaryTableHistoryUpdateAddsHistoryEndAndInsert(EntityManagerFactoryScope scope) {
		final var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort( "Skipping GRAPH test with non-GRAPH queue type" );
		}

		scope.inTransaction( entityManager -> {
			final SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			final HistorySecondaryEntity entity = new HistorySecondaryEntity();
			entity.id = 5L;
			entity.primaryName = "primary-before";
			entity.secondaryName = "secondary-before";
			entityManager.persist( entity );
			entityManager.flush();

			final EntityPersister persister = sfi.getMappingMetamodel().getEntityDescriptor( HistorySecondaryEntity.class );
			final Object[] previousState = persister.getValues( entity );
			entity.primaryName = "primary-after";
			entity.secondaryName = "secondary-after";

			final EntityUpdateAction action = new EntityUpdateAction(
					persister.getIdentifier( entity, session ),
					persister.getValues( entity ),
					allDirty( previousState ),
					false,
					previousState,
					persister.getVersion( entity ),
					null,
					entity,
					null,
					persister,
					(EventSource) session
			);
			final List<FlushOperation> operations = new ArrayList<>();
			persister.getUpdateDecomposer().decompose( action, 0, session, null, operations::add );

			assertEquals( 4, operations.size() );
			assertOperation( operations.get( 0 ), MutationKind.UPDATE, "graph_history_secondary_entity" );
			assertOperation( operations.get( 1 ), MutationKind.UPDATE, "graph_history_secondary_entity_details" );
			assertOperation( operations.get( 2 ), MutationKind.UPDATE, "graph_history_secondary_entity_history" );
			assertOperation( operations.get( 3 ), MutationKind.INSERT, "graph_history_secondary_entity_history" );

			entity.primaryName = "primary-before";
			entity.secondaryName = "secondary-before";
		} );
	}

	@Test
	public void testSecondaryTableHistoryDeleteAddsSingleHistoryEnd(EntityManagerFactoryScope scope) {
		final var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort( "Skipping GRAPH test with non-GRAPH queue type" );
		}

		scope.inTransaction( entityManager -> {
			final SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			final HistorySecondaryEntity entity = new HistorySecondaryEntity();
			entity.id = 6L;
			entity.primaryName = "primary";
			entity.secondaryName = "secondary";
			entityManager.persist( entity );
			entityManager.flush();

			final EntityPersister persister = sfi.getMappingMetamodel().getEntityDescriptor( HistorySecondaryEntity.class );
			final EntityDeleteAction action = new EntityDeleteAction(
					persister.getIdentifier( entity, session ),
					persister.getValues( entity ),
					persister.getVersion( entity ),
					entity,
					persister,
					false,
					(EventSource) session
			);
			final List<FlushOperation> operations = new ArrayList<>();
			persister.getDeleteDecomposer().decompose( action, 0, session, null, operations::add );

			assertEquals( 3, operations.size() );
			assertOperation( operations.get( 0 ), MutationKind.DELETE, "graph_history_secondary_entity_details" );
			assertOperation( operations.get( 1 ), MutationKind.DELETE, "graph_history_secondary_entity" );
			assertOperation( operations.get( 2 ), MutationKind.UPDATE, "graph_history_secondary_entity_history" );
		} );
	}

	@Test
	public void testJoinedHistoryInsertAddsSingleHistoryInsert(EntityManagerFactoryScope scope) {
		final var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort( "Skipping GRAPH test with non-GRAPH queue type" );
		}

		scope.inTransaction( entityManager -> {
			final SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			final EntityPersister persister = sfi.getMappingMetamodel().getEntityDescriptor( HistoryJoinedChild.class );
			final HistoryJoinedChild entity = new HistoryJoinedChild();
			entity.id = 7L;
			entity.parentName = "parent";
			entity.childName = "child";

			final EntityInsertAction action = new EntityInsertAction(
					entity.id,
					persister.getValues( entity ),
					entity,
					persister.getVersion( entity ),
					persister,
					false,
					(EventSource) session
			);
			final List<FlushOperation> operations = new ArrayList<>();
			persister.getInsertDecomposer().decompose( action, 0, session, null, operations::add );

			assertEquals( 3, operations.size() );
			assertOperation( operations.get( 0 ), MutationKind.INSERT, "graph_history_joined_parent" );
			assertOperation( operations.get( 1 ), MutationKind.INSERT, "graph_history_joined_child" );
			assertOperation( operations.get( 2 ), MutationKind.INSERT, "graph_history_joined_parent_history" );
		} );
	}

	@Test
	public void testJoinedHistoryUpdateAddsHistoryEndAndInsert(EntityManagerFactoryScope scope) {
		final var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort( "Skipping GRAPH test with non-GRAPH queue type" );
		}

		scope.inTransaction( entityManager -> {
			final SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			final HistoryJoinedChild entity = new HistoryJoinedChild();
			entity.id = 8L;
			entity.parentName = "parent-before";
			entity.childName = "child-before";
			entityManager.persist( entity );
			entityManager.flush();

			final EntityPersister persister = sfi.getMappingMetamodel().getEntityDescriptor( HistoryJoinedChild.class );
			final Object[] previousState = persister.getValues( entity );
			entity.parentName = "parent-after";
			entity.childName = "child-after";

			final EntityUpdateAction action = new EntityUpdateAction(
					persister.getIdentifier( entity, session ),
					persister.getValues( entity ),
					allDirty( previousState ),
					false,
					previousState,
					persister.getVersion( entity ),
					null,
					entity,
					null,
					persister,
					(EventSource) session
			);
			final List<FlushOperation> operations = new ArrayList<>();
			persister.getUpdateDecomposer().decompose( action, 0, session, null, operations::add );

			assertEquals( 4, operations.size() );
			assertOperation( operations.get( 0 ), MutationKind.UPDATE, "graph_history_joined_parent" );
			assertOperation( operations.get( 1 ), MutationKind.UPDATE, "graph_history_joined_child" );
			assertOperation( operations.get( 2 ), MutationKind.UPDATE, "graph_history_joined_parent_history" );
			assertOperation( operations.get( 3 ), MutationKind.INSERT, "graph_history_joined_parent_history" );

			entity.parentName = "parent-before";
			entity.childName = "child-before";
		} );
	}

	@Test
	public void testJoinedHistoryDeleteAddsSingleHistoryEnd(EntityManagerFactoryScope scope) {
		final var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort( "Skipping GRAPH test with non-GRAPH queue type" );
		}

		scope.inTransaction( entityManager -> {
			final SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			final HistoryJoinedChild entity = new HistoryJoinedChild();
			entity.id = 9L;
			entity.parentName = "parent";
			entity.childName = "child";
			entityManager.persist( entity );
			entityManager.flush();

			final EntityPersister persister = sfi.getMappingMetamodel().getEntityDescriptor( HistoryJoinedChild.class );
			final EntityDeleteAction action = new EntityDeleteAction(
					persister.getIdentifier( entity, session ),
					persister.getValues( entity ),
					persister.getVersion( entity ),
					entity,
					persister,
					false,
					(EventSource) session
			);
			final List<FlushOperation> operations = new ArrayList<>();
			persister.getDeleteDecomposer().decompose( action, 0, session, null, operations::add );

			assertEquals( 3, operations.size() );
			assertOperation( operations.get( 0 ), MutationKind.DELETE, "graph_history_joined_child" );
			assertOperation( operations.get( 1 ), MutationKind.DELETE, "graph_history_joined_parent" );
			assertOperation( operations.get( 2 ), MutationKind.UPDATE, "graph_history_joined_parent_history" );
		} );
	}

	private static void assertOperation(
			FlushOperation operation,
			MutationKind expectedKind,
			String expectedTableExpression) {
		assertEquals( expectedKind, operation.getKind() );
		assertEquals( expectedTableExpression, operation.getTableExpression() );
	}

	private static int[] allDirty(Object[] state) {
		final int[] dirtyFields = new int[state.length];
		for ( int i = 0; i < dirtyFields.length; i++ ) {
			dirtyFields[i] = i;
		}
		return dirtyFields;
	}

	@Entity(name = "GraphHistoryEntity")
	@Table(name = "graph_history_entity")
	@org.hibernate.annotations.Temporal
	public static class HistoryEntity {
		@Id
		Long id;

		String name;
	}

	@Entity(name = "GraphHistorySecondaryEntity")
	@Table(name = "graph_history_secondary_entity")
	@SecondaryTable(name = "graph_history_secondary_entity_details")
	@org.hibernate.annotations.Temporal
	public static class HistorySecondaryEntity {
		@Id
		Long id;

		String primaryName;

		@jakarta.persistence.Column(table = "graph_history_secondary_entity_details")
		String secondaryName;
	}

	@Entity(name = "GraphHistoryJoinedParent")
	@Table(name = "graph_history_joined_parent")
	@Inheritance(strategy = InheritanceType.JOINED)
	@org.hibernate.annotations.Temporal
	public static class HistoryJoinedParent {
		@Id
		Long id;

		String parentName;
	}

	@Entity(name = "GraphHistoryJoinedChild")
	@Table(name = "graph_history_joined_child")
	public static class HistoryJoinedChild extends HistoryJoinedParent {
		String childName;
	}
}

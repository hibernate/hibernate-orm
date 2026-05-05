/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.decomposer;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
 * Tests graph decomposition for temporal updates.
 *
 * @author Steve Ebersole
 */
@Jpa(
		annotatedClasses = TemporalUpdateDecomposerTest.TemporalEntity.class,
		properties = {
				@Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "graph"),
				@Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE")
		}
)
public class TemporalUpdateDecomposerTest {

	@Test
	public void testTemporalUpdateDecomposesToEndAndInsert(EntityManagerFactoryScope scope) {
		final var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort( "Skipping GRAPH test with non-GRAPH queue type" );
		}

		scope.inTransaction( entityManager -> {
			final SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			final TemporalEntity entity = new TemporalEntity();
			entity.id = 1L;
			entity.name = "before";
			entityManager.persist( entity );
			entityManager.flush();

			final EntityPersister persister = sfi.getMappingMetamodel().getEntityDescriptor( TemporalEntity.class );

			final Object[] previousState = persister.getValues( entity );
			entity.name = "after";
			final EntityUpdateAction action = createUpdateAction( entity, previousState, session, persister );
			final List<FlushOperation> operations = new ArrayList<>();
			persister.getUpdateDecomposer().decompose( action, 0, session, null, operations::add );

			assertEquals( 2, operations.size() );
			assertEquals( MutationKind.UPDATE, operations.get( 0 ).getKind() );
			assertEquals( MutationKind.INSERT, operations.get( 1 ).getKind() );

			entity.name = "before";
		} );
	}

	private static EntityUpdateAction createUpdateAction(
			Object entity,
			Object[] previousState,
			SessionImplementor session,
			EntityPersister persister) {
		return new EntityUpdateAction(
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
	}

	@Entity(name = "GraphTemporalUpdateEntity")
	@Table(name = "graph_temporal_update_entity")
	@org.hibernate.annotations.Temporal
	public static class TemporalEntity {
		@Id
		Long id;

		String name;
	}
}

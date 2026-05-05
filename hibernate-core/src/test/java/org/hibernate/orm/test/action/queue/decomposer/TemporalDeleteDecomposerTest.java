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

import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.QueueType;
import org.hibernate.action.queue.plan.FlushOperation;
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
 * Tests graph decomposition for temporal deletes.
 *
 * @author Steve Ebersole
 */
@Jpa(
		annotatedClasses = TemporalDeleteDecomposerTest.TemporalEntity.class,
		properties = {
				@Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "graph"),
				@Setting(name = StateManagementSettings.TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE")
		}
)
public class TemporalDeleteDecomposerTest {

	@Test
	public void testTemporalDeleteDecomposesToUpdate(EntityManagerFactoryScope scope) {
		final var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort( "Skipping GRAPH test with non-GRAPH queue type" );
		}

		scope.inTransaction( entityManager -> {
			final SessionImplementor session = entityManager.unwrap( SessionImplementor.class );
			final TemporalEntity entity = new TemporalEntity();
			entity.id = 1L;
			entity.name = "test";
			entityManager.persist( entity );
			entityManager.flush();

			final EntityPersister persister = sfi.getMappingMetamodel().getEntityDescriptor( TemporalEntity.class );

			final EntityDeleteAction action = createDeleteAction( entity, session, persister );
			final List<FlushOperation> operations = new ArrayList<>();
			persister.getDeleteDecomposer().decompose( action, 0, session, null, operations::add );

			assertEquals( 1, operations.size() );
			assertEquals( MutationKind.UPDATE, operations.get( 0 ).getKind() );
		} );
	}

	private static EntityDeleteAction createDeleteAction(
			Object entity,
			SessionImplementor session,
			EntityPersister persister) {
		return new EntityDeleteAction(
				persister.getIdentifier( entity, session ),
				persister.getValues( entity ),
				persister.getVersion( entity ),
				entity,
				persister,
				false,
				(EventSource) session
		);
	}

	@Entity(name = "GraphTemporalDeleteEntity")
	@Table(name = "graph_temporal_delete_entity")
	@org.hibernate.annotations.Temporal
	public static class TemporalEntity {
		@Id
		Long id;

		String name;
	}
}

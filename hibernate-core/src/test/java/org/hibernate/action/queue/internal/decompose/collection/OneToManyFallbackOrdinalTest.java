/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.action.queue.spi.decompose.collection.CollectionJdbcOperations;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationPlanContributor;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.queue.spi.decompose.DecompositionContext;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.OneToManyPersister;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/// Tests that every part of a one-to-many fallback comparison uses the range
/// allocated to the enclosing collection mutation.
///
/// @author Steve Ebersole
class OneToManyFallbackOrdinalTest {
	@Test
	void fallbackUsesOneOrdinalBase() {
		final var decomposer = new RecordingDecomposer();
		decomposer.applyFallbackUpdate(
				mock( PersistentCollection.class ),
				1,
				7,
				mock( SharedSessionContractImplementor.class ),
				operation -> {
				}
		);

		assertEquals( List.of( 7, 7, 7 ), decomposer.ordinalBases );
	}

	private static class RecordingDecomposer extends AbstractOneToManyDecomposer {
		private final List<Integer> ordinalBases = new ArrayList<>();

		private RecordingDecomposer() {
			super(
					mock( OneToManyPersister.class ),
					mock( SessionFactoryImplementor.class ),
					CollectionMutationPlanContributor.STANDARD
			);
		}

		@Override
		protected CollectionJdbcOperations selectJdbcOperations(
				Object entry,
				SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public void decomposeRemove(
				CollectionRemoveAction action,
				int ordinalBase,
				SharedSessionContractImplementor session,
				DecompositionContext decompositionContext,
				Consumer<FlushOperation> operationConsumer) {
		}

		@Override
		protected void applyUpdateRemovals(
				PersistentCollection<?> collection,
				Object key,
				int ordinalBase,
				SharedSessionContractImplementor session,
				Consumer<FlushOperation> operationConsumer) {
			ordinalBases.add( ordinalBase );
		}

		@Override
		protected void applyUpdateChanges(
				PersistentCollection<?> collection,
				Object key,
				int ordinalBase,
				SharedSessionContractImplementor session,
				Consumer<FlushOperation> operationConsumer) {
			ordinalBases.add( ordinalBase );
		}

		@Override
		protected void applyUpdateAdditions(
				PersistentCollection<?> collection,
				Object key,
				int ordinalBase,
				SharedSessionContractImplementor session,
				Consumer<FlushOperation> operationConsumer) {
			ordinalBases.add( ordinalBase );
		}
	}
}

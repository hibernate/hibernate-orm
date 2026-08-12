/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.collection;

import org.hibernate.action.queue.internal.PreparedCollectionMutation;
import org.hibernate.action.queue.spi.decompose.DecompositionContext;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationPlanContributor;
import org.hibernate.action.queue.spi.meta.CollectionTableDescriptor;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.BasicValuedCollectionPart;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.persister.collection.BasicCollectionPersister;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests the graph-safety boundary for compact collection-row execution.
///
/// @author Steve Ebersole
class CollectionRowCompactionTest {
	@Test
	void compactsCreateRowsOnlyForAnOwnerInsertedInTheCurrentFlush() {
		final var fixture = new Fixture();
		final var mutation = mock( PreparedCollectionMutation.class );
		final var context = mock( DecompositionContext.class );
		final var owner = new Object();
		when( mutation.getAffectedOwner() ).thenReturn( owner );

		assertThat( fixture.policy.canCompactCreate( mutation, context ) ).isFalse();

		when( context.isBeingInsertedInCurrentFlush( owner ) ).thenReturn( true );
		assertThat( fixture.policy.canCompactCreate( mutation, context ) ).isTrue();
	}

	@Test
	void retainsExpandedRowsForUniqueSlotsCyclesAndContributors() {
		assertThat( new Fixture( true, false, CollectionMutationPlanContributor.STANDARD )
				.policy.canCompactReplacement() ).isFalse();
		assertThat( new Fixture( false, true, CollectionMutationPlanContributor.STANDARD )
				.policy.canCompactReplacement() ).isFalse();
		assertThat( new Fixture( false, false, mock( CollectionMutationPlanContributor.class ) )
				.policy.canCompactReplacement() ).isFalse();
	}

	@Test
	void retainsExpandedRowsForEntityValuedCollections() {
		final var fixture = new Fixture(
				false,
				false,
				CollectionMutationPlanContributor.STANDARD,
				mock( EntityCollectionPart.class )
		);

		assertThat( fixture.policy.canCompactReplacement() ).isFalse();
	}

	private static class Fixture {
		private final BasicCollectionPersister persister = mock( BasicCollectionPersister.class );
		private final PluralAttributeMapping attributeMapping = mock( PluralAttributeMapping.class );
		private final CollectionRowCompaction policy;

		private Fixture() {
			this( false, false, CollectionMutationPlanContributor.STANDARD );
		}

		private Fixture(
				boolean nonPrimaryUniqueConstraint,
				boolean selfReferential,
				CollectionMutationPlanContributor contributor) {
			this(
					nonPrimaryUniqueConstraint,
					selfReferential,
					contributor,
					mock( BasicValuedCollectionPart.class )
			);
		}

		private Fixture(
				boolean nonPrimaryUniqueConstraint,
				boolean selfReferential,
				CollectionMutationPlanContributor contributor,
				CollectionPart elementDescriptor) {
			final var table = mock( CollectionTableDescriptor.class );
			when( table.isSelfReferential() ).thenReturn( selfReferential );
			when( persister.getCollectionTableDescriptor() ).thenReturn( table );
			when( persister.getAttributeMapping() ).thenReturn( attributeMapping );
			when( attributeMapping.getElementDescriptor() ).thenReturn( elementDescriptor );
			policy = new CollectionRowCompaction( persister, contributor, nonPrimaryUniqueConstraint );
		}
	}
}

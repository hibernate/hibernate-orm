/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.collection;

import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.decompose.collection.CollectionJdbcOperations;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.collection.spi.FrozenCollectionRows;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests immutable row selection from a compact collection insert binding group.
///
/// @author Steve Ebersole
class GroupedCollectionInsertBindPlanTest {
	@Test
	void bindsEveryFrozenRowByItsPositionInTheGroup() {
		final var persister = mock( CollectionPersister.class );
		final var values = mock( CollectionJdbcOperations.Values.class );
		final var collection = mock( PersistentCollection.class );
		final var key = 1L;
		final var rows = mock( FrozenCollectionRows.class );
		when( rows.size() ).thenReturn( 2 );
		when( rows.entry( 0 ) ).thenReturn( "first" );
		when( rows.entry( 1 ) ).thenReturn( "second" );
		when( rows.position( 0 ) ).thenReturn( 3 );
		when( rows.position( 1 ) ).thenReturn( 7 );
		final var plan = new GroupedCollectionInsertBindPlan(
				persister,
				values,
				collection,
				key,
				rows
		);
		final var jdbcBindings = mock( JdbcValueBindings.class );
		final var operation = mock( FlushOperation.class );
		final var session = mock( SharedSessionContractImplementor.class );

		assertThat( plan.getBindingCount() ).isEqualTo( 2 );
		plan.bindValues( 0, jdbcBindings, operation, session );
		plan.bindValues( 1, jdbcBindings, operation, session );

		verify( values ).applyValues( collection, key, "first", 3, session, jdbcBindings );
		verify( values ).applyValues( collection, key, "second", 7, session, jdbcBindings );
	}
}

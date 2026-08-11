/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.bind;

import org.hibernate.Incubating;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/// A binding plan whose immutable row bindings share one physical operation
/// template and graph placement.
///
/// The executor still performs one JDBC mutation for every binding. Grouping
/// avoids materializing a separate [FlushOperation] and [BindPlan] for each row;
/// it does not combine rows into one SQL statement or one JDBC result.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public interface GroupedRowBindPlan extends BindPlan {
	/// The number of immutable row bindings represented by this plan.
	int getBindingCount();

	/// Bind one row identified by its zero-based position in the immutable group.
	void bindValues(
			int bindingIndex,
			JdbcValueBindings valueBindings,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session);

	@Override
	default void bindValues(
			JdbcValueBindings valueBindings,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session) {
		if ( getBindingCount() != 1 ) {
			throw new IllegalStateException( "Grouped row binding requires an explicit binding index" );
		}
		bindValues( 0, valueBindings, flushOperation, session );
	}
}

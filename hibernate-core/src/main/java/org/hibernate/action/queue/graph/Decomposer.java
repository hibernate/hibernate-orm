/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;

import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.action.spi.Executable;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SessionImplementor;

import java.util.List;

/// Manages decomposition of [actions][Executable] into table operations for planning.
///
/// @implNote Ordering and scheduling are external concerns handled later.
///
/// @author Steve Ebersole
public class Decomposer {
	private final SessionImplementor session;
	private final MutationExecutorService executorService;

	public Decomposer(SessionImplementor session) {
		this.session = session;
		this.executorService = session.getFactory()
				.getServiceRegistry()
				.requireService( MutationExecutorService.class );
	}

	public List<PlannedOperationGroup> decompose(Executable executable, int ordinalBase) {
		if (executable instanceof EntityInsertAction eia) {
			return eia.getPersister().getInsertDecomposer().decompose( eia, ordinalBase, session );
		}
		if (executable instanceof EntityUpdateAction eua) {
			return eua.getPersister().getUpdateDecomposer().decompose( eua, ordinalBase, session );
		}
		if (executable instanceof EntityDeleteAction eda) {
			return eda.getPersister().getDeleteDecomposer().decompose( eda, ordinalBase, session );
		}

		if (executable instanceof CollectionRecreateAction cra) {
			// MutationKind.INSERT
			return cra.getPersister().getRecreateDecomposer().decompose( cra, ordinalBase, session );
		}
		if (executable instanceof CollectionRemoveAction cra) {
			// MutationKind.DELETE
			return cra.getPersister().getRemoveDecomposer().decompose( cra, ordinalBase, session );
		}
		if (executable instanceof CollectionUpdateAction cua) {
			return cua.getPersister().getUpdateDecomposer().decompose( cua, ordinalBase, session );
		}

		throw new UnsupportedOperationException( "Decomposition not supported for " +  executable.getClass().getName() );
	}
}

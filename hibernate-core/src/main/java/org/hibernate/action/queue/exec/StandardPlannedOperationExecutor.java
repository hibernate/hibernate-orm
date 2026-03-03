/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.hibernate.action.queue.bind.DeferredJdbcValueBindings;
import org.hibernate.action.queue.cyclebreak.FkFixupUpdateFactory;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.spi.BatchKeyAccess;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * @author Steve Ebersole
 */
public class StandardPlannedOperationExecutor implements PlannedOperationExecutor {
	private final FkFixupUpdateFactory nullableFkUpdateFactory;
	private final boolean requireDeferredBindings;
	private final SharedSessionContractImplementor session;
	private final MutationExecutorService executorService;


	public StandardPlannedOperationExecutor(
			FkFixupUpdateFactory nullableFkUpdateFactory,
			boolean requireDeferredBindings,
			SharedSessionContractImplementor session) {
		this.nullableFkUpdateFactory = nullableFkUpdateFactory;
		this.requireDeferredBindings = requireDeferredBindings;
		this.session = session;
		this.executorService = session.getFactory()
				.getServiceRegistry()
				.requireService( MutationExecutorService.class );
	}

	@Override
	public void executePlannedOperation(PlannedOperation op) {
		// Wrap operation as a single-op group internally (planner never sees this)
		final MutationOperationGroup singleGroup = new SingleOperationGroup(op, op.getKind());

		final MutationExecutor exec = executorService.createExecutor(
				batchKeySupplier(op),
				singleGroup,
				session
		);

		if (requireDeferredBindings) {
			ensureDeferredJdbcValueBindings(exec);
		}

		op.getBindPlan().bindAndMaybePatch(exec, op, session );
		op.getBindPlan().execute(exec, op, session );
	}

	@Override
	public PlannedOperation synthesizeFixupUpdateIfNeeded(PlannedOperation cycleBrokenInsertOp, Object entityId) {
//		if (cycleBrokenInsertOp.getIntendedFkValues().isEmpty()) {
//			return null;
//		}
//		if (entityId == null) {
//			throw new IllegalStateException("FK fixup requires non-null entityId (identity prereq must have executed)");
//		}
//
//		final String table = cycleBrokenInsertOp.getTableExpression();
//
//		final FkFixupUpdateFactory.UpdateTemplate tmpl = nullableFkUpdateFactory.buildFkFixupUpdateGroup(
//				table,
//				cycleBrokenInsertOp.getIntendedFkValues().keySet(),
//				session
//		);
//
//		final BindPlan bindPlan = new FkFixupUpdateBindPlan(
//				nullableFkUpdateFactory.entityPersister,
//				entityId,
//				cycleBrokenInsertOp.getIntendedFkValues(),
//				tmpl
//		);
//
//		return new PlannedOperation(
//				table,
//				MutationKind.UPDATE,
//				tmpl.operation(),
//				bindPlan,
//				cycleBrokenInsertOp.getOrdinal() + 10_000,
//				cycleBrokenInsertOp.getOrigin() + " [cycle-break FK fixup update]"
//		);
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	private BatchKeyAccess batchKeySupplier(PlannedOperation op) {
		return () -> new BasicBatchKey(op.getTableExpression() + "#" + op.getKind().name());
	}

	private void ensureDeferredJdbcValueBindings(MutationExecutor exec) {
		final JdbcValueBindings cur = exec.getJdbcValueBindings();
		if (cur instanceof DeferredJdbcValueBindings ) {
			return;
		}
		throw new IllegalStateException("Expecting MutationExecutor#getJdbcValueBindings to be a DeferredJdbcValueBindings");
	}
}

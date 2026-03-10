/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.hibernate.action.queue.Helper;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.StatementShapeKey;
import org.hibernate.action.queue.cyclebreak.FkFixupUpdateBindPlan;
import org.hibernate.action.queue.cyclebreak.FkFixupUpdateFactory;
import org.hibernate.action.queue.cyclebreak.UniqueSwapUpdateBindPlan;
import org.hibernate.action.queue.cyclebreak.UniqueSwapUpdateFactory;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.spi.BatchKeyAccess;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * @author Steve Ebersole
 */
public class StandardPlannedOperationExecutor implements PlannedOperationExecutor {
	private final FkFixupUpdateFactory nullableFkUpdateFactory;
	private final UniqueSwapUpdateFactory uniqueSwapUpdateFactory;
	private final SharedSessionContractImplementor session;
	private final MutationExecutorService executorService;


	public StandardPlannedOperationExecutor(
			FkFixupUpdateFactory nullableFkUpdateFactory,
			SharedSessionContractImplementor session) {
		this.nullableFkUpdateFactory = nullableFkUpdateFactory;
		this.uniqueSwapUpdateFactory = new UniqueSwapUpdateFactory();
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

		op.getBindPlan().bindAndMaybePatch(exec, op, session );
		op.getBindPlan().execute(exec, op, session );
	}

	@Override
	public PlannedOperation synthesizeFixupUpdateIfNeeded(PlannedOperation cycleBrokenInsertOp, Object entityId) {
		// No fixup needed if no FK values were deferred during cycle breaking
		if (cycleBrokenInsertOp.getIntendedFkValues().isEmpty()) {
			return null;
		}

		if (entityId == null) {
			throw new IllegalStateException("FK fixup requires non-null entityId (identity prereq must have executed)");
		}

		var mutationTarget = cycleBrokenInsertOp.getOperation().getMutationTarget();
		var persister = ( mutationTarget instanceof EntityMutationTarget emt )
				? emt.getTargetPart().getEntityPersister()
				: null;
		if ( persister == null ) {
			throw new IllegalStateException("FK fixup only valid for entities, but found - " + mutationTarget);
		}

		final String table = cycleBrokenInsertOp.getTableExpression();
		assert table != null;
		assert table.equals( Helper.normalizeTableName(table) );

		final FkFixupUpdateFactory.UpdateTemplate tmpl = nullableFkUpdateFactory.buildFkFixupUpdateGroup(
				persister,
				table,
				cycleBrokenInsertOp.getIntendedFkValues().keySet(),
				session
		);

		final FkFixupUpdateBindPlan bindPlan = new FkFixupUpdateBindPlan(
				persister,
				entityId,
				cycleBrokenInsertOp.getIntendedFkValues(),
				tmpl
		);

		return new PlannedOperation(
				table,
				MutationKind.UPDATE,
				tmpl.operation(),
				bindPlan,
				cycleBrokenInsertOp.getOrdinal() + 10_000,
				cycleBrokenInsertOp.getOrigin() + " [cycle-break FK fixup update]"
		);
	}

	public PlannedOperation synthesizeUniqueSwapUpdateIfNeeded(PlannedOperation cycleBrokenUpdateOp, Object entityId) {
		// No fixup needed if no unique constraint values were deferred during cycle breaking
		if (cycleBrokenUpdateOp.getIntendedUniqueValues().isEmpty()) {
			return null;
		}

		if (entityId == null) {
			throw new IllegalStateException("Unique swap fixup requires non-null entityId");
		}

		var mutationTarget = cycleBrokenUpdateOp.getOperation().getMutationTarget();
		var persister = ( mutationTarget instanceof EntityMutationTarget emt )
				? emt.getTargetPart().getEntityPersister()
				: null;
		if ( persister == null ) {
			throw new IllegalStateException("Unique swap fixup only valid for entities, but found - " + mutationTarget);
		}

		final String table = cycleBrokenUpdateOp.getTableExpression();
		assert table != null;
		assert table.equals( Helper.normalizeTableName(table) );

		final UniqueSwapUpdateFactory.UpdateTemplate tmpl = uniqueSwapUpdateFactory.buildUniqueSwapUpdateGroup(
				persister,
				table,
				cycleBrokenUpdateOp.getIntendedUniqueValues().keySet(),
				session
		);

		final UniqueSwapUpdateBindPlan bindPlan = new UniqueSwapUpdateBindPlan(
				persister,
				entityId,
				cycleBrokenUpdateOp.getIntendedUniqueValues(),
				tmpl
		);

		return new PlannedOperation(
				table,
				MutationKind.UPDATE,
				tmpl.operation(),
				bindPlan,
				cycleBrokenUpdateOp.getOrdinal() + 10_000,
				cycleBrokenUpdateOp.getOrigin() + " [cycle-break unique swap fixup update]"
		);
	}

	private BatchKeyAccess batchKeySupplier(PlannedOperation op) {
		// Use StatementShapeKey which includes table, operation type, and SQL structure hash
		// This ensures operations with different SQL structures get separate batches
		// Example: "DELETE WHERE id=?" and "DELETE WHERE fk=? AND id=?" targeting the same table
		// will use different batches due to different parameter counts
		final StatementShapeKey shapeKey = switch (op.getKind()) {
			case INSERT -> StatementShapeKey.forInsert(op.getTableExpression(), op);
			case UPDATE -> StatementShapeKey.forUpdate(op.getTableExpression(), op);
			case DELETE -> StatementShapeKey.forDelete(op.getTableExpression(), op);
		};
		return () -> shapeKey;
	}
}

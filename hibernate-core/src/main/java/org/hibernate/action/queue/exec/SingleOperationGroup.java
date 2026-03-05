/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;

/**
 * @author Steve Ebersole
 */
public final class SingleOperationGroup implements MutationOperationGroup {
	private final MutationOperationGroup template;
	private final PlannedOperation operation;
	private final MutationKind localizedKind;

	public SingleOperationGroup(MutationOperationGroup template, PlannedOperation operation, MutationKind mutationTypeOverride) {
		this.template = template;
		this.operation = operation;
		this.localizedKind = mutationTypeOverride;
	}

	public SingleOperationGroup(PlannedOperation operation, MutationKind kind) {
		this.template = null;
		this.operation = operation;
		this.localizedKind = kind == MutationKind.INSERT
				? MutationKind.INSERT
				: kind == MutationKind.UPDATE ? MutationKind.UPDATE : MutationKind.DELETE;
	}

	@Override
	public MutationType getMutationType() {
		return switch ( localizedKind ) {
			case INSERT -> MutationType.INSERT;
			case UPDATE -> MutationType.UPDATE;
			case DELETE -> MutationType.DELETE;
		};
	}

	@Override
	public MutationTarget<?> getMutationTarget() {
		return template.getMutationTarget();
	}

	@Override
	public int getNumberOfOperations() {
		return 1;
	}

	@Override
	public MutationOperation getOperation(int position) {
		if (position != 0) throw new IndexOutOfBoundsException(position);
		return operation.getOperation();
	}

	@Override
	public MutationOperation getSingleOperation() {
		return operation.getOperation();
	}

	@Override
	public MutationOperation getOperation(String tableName) {
		if ( operation.getOperation().getTableDetails().getTableName().equals( tableName ) ) {
			return operation.getOperation();
		}
		throw new IllegalArgumentException( "Table name does not match: " + tableName );
	}
}

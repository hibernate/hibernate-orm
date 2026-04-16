/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.queue.decompose.DecompositionContext;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.ast.TableMutation;

import java.util.List;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
public interface DeleteDecomposer extends EntityActionDecomposer<EntityDeleteAction> {
	Map<String, ? extends TableMutation<?>> getStaticDeleteOperations();

	List<PlannedOperation> decompose(
			EntityDeleteAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext);
}

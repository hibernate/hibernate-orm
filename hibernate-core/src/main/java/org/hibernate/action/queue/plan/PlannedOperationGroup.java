/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.plan;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.StatementShapeKey;

import java.util.List;

/// Groups all [operations][#operations] of a given [kind][#kind] for a given [table][#tableExpression],
/// specifically for a certain ["shape"][#shapeKey].
///
/// @author Steve Ebersole
public record PlannedOperationGroup(
		String tableExpression,
		MutationKind kind,
		StatementShapeKey shapeKey,
		List<PlannedOperation> operations,
		boolean needsIdPrePhase,
		int ordinal,
		String origin) {
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.plan;

import org.hibernate.action.queue.spi.plan.FlushOperation;

import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.StatementShapeKey;
import org.hibernate.action.queue.spi.plan.FlushOperation;

import java.util.List;

/// Groups [operations][#operations] of a given [kind][#kind] and [shape][#shape]
/// against a [table][#tableExpression].
///
/// For example, we would group -
///
/// - insert into persons (name, id) values (?,?)
/// - insert into persons (name, id) values (?,?)
/// - ...
///
/// But this group would not contain any of
///
/// - insert into persons (id) values (?) // different shape
/// - insert into addresses ... // different table
/// - update persons ... // different kind
///
/// @author Steve Ebersole
public record FlushOperationGroup(
		String tableExpression,
		MutationKind kind,
		StatementShapeKey shapeKey,
		List<FlushOperation> operations,
		boolean needsIdPrePhase,
		boolean hasUniqueConstraints,
		int ordinal,
		String origin) {
}

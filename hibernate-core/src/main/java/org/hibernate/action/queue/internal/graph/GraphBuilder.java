/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.graph;


import org.hibernate.action.queue.internal.constraint.DeferrableConstraintMode;
import org.hibernate.action.queue.internal.plan.FlushOperationGroup;

import java.util.List;

/// Constructs a directed dependency [graph][Graph] from Flush operations using details
/// from the [org.hibernate.action.queue.internal.constraint.ConstraintModel].
///
/// @author Steve Ebersole
public interface GraphBuilder {
	Graph build(List<FlushOperationGroup> groups, DeferrableConstraintMode deferrableConstraintMode);
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.plan;

import java.util.List;

/// Simple implementation of PlanStep
///
/// @author Steve Ebersole
public record SimplePlanStep(List<PlannedOperation> operations) implements PlanStep {
}

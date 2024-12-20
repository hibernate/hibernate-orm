/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

/**
 * Something that exposes a MutabilityPlan
 */
public interface MutabilityPlanExposer<T> {
	MutabilityPlan<T> getExposedMutabilityPlan();
}

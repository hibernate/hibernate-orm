package org.hibernate.type.descriptor.java;

/**
 * Something that exposes a MutabilityPlan
 */
public interface MutabilityPlanExposer<T> {
	MutabilityPlan<T> getExposedMutabilityPlan();
}

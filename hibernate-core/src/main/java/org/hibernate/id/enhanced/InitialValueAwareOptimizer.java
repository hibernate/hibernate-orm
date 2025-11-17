/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

/**
 * Marker interface for optimizer which wishes to know the user-specified initial value.
 * <p>
 * Used instead of constructor injection since that is already a public understanding and
 * because not all optimizers care.
 *
 * @author Steve Ebersole
 */
public interface InitialValueAwareOptimizer {
	/**
	 * Reports the user specified initial value to the optimizer.
	 * <p>
	 * {@code -1} is used to indicate that the user did not specify.
	 *
	 * @param initialValue The initial value specified by the user,
	 *                     or {@code -1} to indicate that the user
	 *                     did not specify an initial value.
	 */
	void injectInitialValue(long initialValue);
}

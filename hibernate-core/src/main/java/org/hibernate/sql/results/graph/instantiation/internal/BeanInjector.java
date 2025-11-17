/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.instantiation.internal;

/**
 * Unified contract for injecting a single argument for a dynamic instantiation
 * result, whether that is constructor-based or setter-based.
 *
 * @author Steve Ebersole
 */
interface BeanInjector<T> {
	void inject(T target, Object value);
}

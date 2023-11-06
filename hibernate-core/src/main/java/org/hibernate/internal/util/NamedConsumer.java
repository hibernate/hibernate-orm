/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface NamedConsumer<T> {
	void consume(String name, T thing);
}

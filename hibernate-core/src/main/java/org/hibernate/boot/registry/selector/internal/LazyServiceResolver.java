/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.selector.internal;

@FunctionalInterface
public interface LazyServiceResolver<T> {

	Class<? extends T> resolve(String name);

}

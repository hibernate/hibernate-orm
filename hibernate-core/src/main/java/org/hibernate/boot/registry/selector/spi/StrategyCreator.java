/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.selector.spi;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface StrategyCreator<T> {
	T create(Class<? extends T> strategyClass);
}

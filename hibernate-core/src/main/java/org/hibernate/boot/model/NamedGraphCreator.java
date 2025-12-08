/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;

import java.util.function.Function;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface NamedGraphCreator {
	RootGraphImplementor<?> createEntityGraph(
			Function<Class<?>, EntityDomainType<?>> entityDomainClassResolver,
			Function<String, EntityDomainType<?>> entityDomainNameResolver);
}

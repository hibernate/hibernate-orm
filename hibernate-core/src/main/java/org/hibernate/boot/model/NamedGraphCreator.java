/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
	<T> RootGraphImplementor<T> createEntityGraph(
			Function<Class<T>, EntityDomainType<?>> entityDomainClassResolver,
			Function<String, EntityDomainType<?>> entityDomainNameResolver);
}

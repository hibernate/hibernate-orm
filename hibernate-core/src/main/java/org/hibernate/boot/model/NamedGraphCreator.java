/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import org.hibernate.graph.spi.GraphParserEntityClassResolver;
import org.hibernate.graph.spi.GraphParserEntityNameResolver;
import org.hibernate.graph.spi.RootGraphImplementor;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface NamedGraphCreator {
	RootGraphImplementor<?> createEntityGraph(
			GraphParserEntityClassResolver entityDomainClassResolver,
			GraphParserEntityNameResolver entityDomainNameResolver);
}

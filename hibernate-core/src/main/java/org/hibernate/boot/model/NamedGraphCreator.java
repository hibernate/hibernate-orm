/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import java.io.Serializable;

import org.hibernate.graph.spi.GraphParserEntityClassResolver;
import org.hibernate.graph.spi.GraphParserEntityNameResolver;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.models.spi.ModelsContext;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface NamedGraphCreator extends Serializable {
	RootGraphImplementor<?> createEntityGraph(
			GraphParserEntityClassResolver entityDomainClassResolver,
			GraphParserEntityNameResolver entityDomainNameResolver,
			ServiceRegistry serviceRegistry);

	default void reattachModelsContext(ModelsContext modelsContext) {
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import java.util.function.Supplier;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationContext;

/**
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface AssemblerCreationState {

	default boolean isDynamicInstantiation() {
		return false;
	}

	default boolean containsMultipleCollectionFetches() {
		return true;
	}

	int acquireInitializerId();

	Initializer<?> resolveInitializer(
			NavigablePath navigablePath,
			ModelPart fetchedModelPart,
			Supplier<Initializer<?>> producer);

	<P extends FetchParent> Initializer<?> resolveInitializer(
			P resultGraphNode,
			InitializerParent<?> parent,
			InitializerProducer<P> producer);

	SqlAstCreationContext getSqlAstCreationContext();

}

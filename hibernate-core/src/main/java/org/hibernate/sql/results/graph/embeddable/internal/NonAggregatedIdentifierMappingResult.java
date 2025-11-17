/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;

public class NonAggregatedIdentifierMappingResult<T> extends EmbeddableResultImpl<T> {
	public NonAggregatedIdentifierMappingResult(
			NavigablePath navigablePath,
			NonAggregatedIdentifierMapping modelPart,
			String resultVariable,
			DomainResultCreationState creationState) {
		super( navigablePath, modelPart, resultVariable, creationState );
	}

	@Override
	public Initializer<?> createInitializer(
			EmbeddableResultImpl<T> resultGraphNode,
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return new NonAggregatedIdentifierMappingInitializer( resultGraphNode, parent, creationState, true );
	}
}

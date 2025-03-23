/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;

public class NonAggregatedIdentifierMappingFetch extends EmbeddableFetchImpl {
	public NonAggregatedIdentifierMappingFetch(
			NavigablePath navigablePath,
			NonAggregatedIdentifierMapping embeddedPartDescriptor,
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean hasTableGroup,
			DomainResultCreationState creationState) {
		super( navigablePath, embeddedPartDescriptor, fetchParent, fetchTiming, hasTableGroup, creationState );
	}

	public NonAggregatedIdentifierMappingFetch(EmbeddableFetchImpl original) {
		super( original );
	}

	@Override
	public EmbeddableInitializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return new NonAggregatedIdentifierMappingInitializer( this, parent, creationState, false );
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;

public class NonAggregatedIdentifierMappingResult<T> extends EmbeddableResultImpl<T> {
	public NonAggregatedIdentifierMappingResult(
			NavigablePath navigablePath,
			NonAggregatedIdentifierMapping modelPart,
			String resultVariable,
			DomainResultCreationState creationState) {
		super( navigablePath, modelPart, resultVariable, creationState );
	}

	@Override
	public DomainResultAssembler<T> createResultAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final EmbeddableInitializer initializer = creationState.resolveInitializer(
				getNavigablePath().append( "{embeddable_result}" ),
				getReferencedModePart(),
				() -> new NonAggregatedIdentifierMappingResultInitializer(
						this,
						parentAccess,
						creationState
				)
		).asEmbeddableInitializer();

		assert initializer != null;

		//noinspection unchecked
		return new EmbeddableAssembler( initializer );
	}
}

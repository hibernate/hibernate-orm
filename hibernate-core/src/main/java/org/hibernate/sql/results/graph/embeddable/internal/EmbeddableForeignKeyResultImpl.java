/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;

/**
 * @author Andrea Boriero
 */
public class EmbeddableForeignKeyResultImpl<T>
		extends AbstractFetchParent
		implements EmbeddableResultGraphNode, DomainResult<T> {

	private final String resultVariable;

	public EmbeddableForeignKeyResultImpl(
			NavigablePath navigablePath,
			EmbeddableValuedModelPart embeddableValuedModelPart,
			String resultVariable,
			DomainResultCreationState creationState) {
		super( embeddableValuedModelPart.getEmbeddableTypeDescriptor(), navigablePath );
		this.resultVariable = resultVariable;
		this.fetches = creationState.visitFetches( this );
	}

	@Override
	public boolean containsAnyNonScalarResults() {
		return true;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public Fetch generateFetchableFetch(
			Fetchable fetchable,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final boolean shouldSelect;
		if ( fetchable instanceof ToOneAttributeMapping ) {
			// We need to make sure to-ones are always delayed to avoid cycles while resolving entity keys
			final ToOneAttributeMapping toOne = (ToOneAttributeMapping) fetchable;
			shouldSelect = selected && !creationState.isAssociationKeyVisited(
					toOne.getForeignKeyDescriptor().getAssociationKey()
			) && !ForeignKeyDescriptor.PART_NAME.equals( getNavigablePath().getLocalName() )
					&& !ForeignKeyDescriptor.TARGET_PART_NAME.equals( getNavigablePath().getLocalName() );
		}
		else {
			shouldSelect = selected;
		}
		return fetchable.generateFetch(
				this,
				fetchablePath,
				fetchTiming,
				shouldSelect,
				resultVariable,
				creationState
		);
	}

	@Override
	public DomainResultAssembler<T> createResultAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final EmbeddableInitializer initializer = (EmbeddableInitializer) creationState.resolveInitializer(
				getNavigablePath(),
				getReferencedModePart(),
				() -> new EmbeddableResultInitializer( this, parentAccess, creationState )
		);

		//noinspection unchecked
		return new EmbeddableAssembler( initializer );
	}

	@Override
	public EmbeddableMappingType getReferencedMappingType() {
		return (EmbeddableMappingType) getFetchContainer().getPartMappingType();
	}

	@Override
	public Fetch findFetch(Fetchable fetchable) {
		return super.findFetch( fetchable );
	}

	@Override
	public EmbeddableMappingType getFetchContainer() {
		return (EmbeddableMappingType) super.getFetchContainer();
	}

	@Override
	public EmbeddableValuedModelPart getReferencedMappingContainer() {
		return getFetchContainer().getEmbeddedValueMapping();
	}
}

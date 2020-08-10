/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.FetchTiming;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.entity.internal.EntityDelayedFetchImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchSelectImpl;

/**
 * @author Andrea Boriero
 */
public class EmbeddableForeignKeyResultImpl<T>
		extends AbstractFetchParent
		implements EmbeddableResultGraphNode, DomainResult<T> {

	private static final String ROLE_LOCAL_NAME = "{fk}";
	private final String resultVariable;

	public EmbeddableForeignKeyResultImpl(
			List<SqlSelection> sqlSelections,
			NavigablePath navigablePath,
			EmbeddableValuedModelPart embeddableValuedModelPart,
			String resultVariable,
			DomainResultCreationState creationState) {
		super( embeddableValuedModelPart.getEmbeddableTypeDescriptor(), navigablePath.append( ROLE_LOCAL_NAME ) );
		this.resultVariable = resultVariable;
		fetches = new ArrayList<>();
		MutableInteger index = new MutableInteger();

		embeddableValuedModelPart.visitFetchables(
				fetchable ->
						generateFetches( sqlSelections, navigablePath, creationState, index, fetchable )
				,
				null
		);
	}

	@Override
	public boolean containsAnyNonScalarResults() {
		return true;
	}

	private void generateFetches(
			List<SqlSelection> sqlSelections,
			NavigablePath navigablePath,
			DomainResultCreationState creationState,
			MutableInteger mutableInteger,
			Fetchable fetchable) {
		if ( fetchable instanceof ToOneAttributeMapping ) {
			final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) fetchable;
			EntityMappingType associatedEntityMappingType = toOneAttributeMapping.getAssociatedEntityMappingType();
			BasicResult domainResult = new BasicResult(
					sqlSelections.get( mutableInteger.getAndIncrement() ).getValuesArrayPosition(),
					null,
					associatedEntityMappingType.getIdentifierMapping().getJavaTypeDescriptor()
			);
			Fetch fetch;
			if ( toOneAttributeMapping.getMappedFetchOptions().getTiming() == FetchTiming.DELAYED ) {
				fetch = new EntityDelayedFetchImpl(
						this,
						toOneAttributeMapping,
						navigablePath.append( fetchable.getFetchableName() ),
						domainResult
				);
			}
			else {
				fetch = new EntityFetchSelectImpl(
						this,
						toOneAttributeMapping,
						false,
						navigablePath.append( fetchable.getFetchableName() ),
						domainResult,
						false,
						creationState
				);
			}
			fetches.add( fetch );
		}
		else {
			final Fetch fetch = new BasicFetch(
					sqlSelections.get( mutableInteger.getAndIncrement() ).getValuesArrayPosition(),
					null,
					navigablePath.append( fetchable.getFetchableName() ),
					(BasicValuedModelPart) fetchable,
					true,
					null,
					FetchTiming.IMMEDIATE,
					creationState
			);
			fetches.add( fetch );
		}
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public DomainResultAssembler<T> createResultAssembler(AssemblerCreationState creationState) {
		final EmbeddableInitializer initializer = (EmbeddableInitializer) creationState.resolveInitializer(
				getNavigablePath(),
				getReferencedModePart(),
				() -> new EmbeddableResultInitializer(this, creationState )
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

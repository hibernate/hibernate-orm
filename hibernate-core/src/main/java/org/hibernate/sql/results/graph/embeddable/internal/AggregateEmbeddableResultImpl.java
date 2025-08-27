/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.hibernate.internal.util.NullnessUtil;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.InitializerProducer;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.embeddable.AggregateEmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResult;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.sql.results.graph.embeddable.AggregateEmbeddableResultGraphNode.determineAggregateValuesArrayPositions;

/**
 * A Result for an embeddable that is mapped as aggregate e.g. STRUCT, JSON or XML.
 * This is only used when {@link EmbeddableMappingType#shouldSelectAggregateMapping()} returns <code>true</code>.
 * The main difference is that it selects only the aggregate column and
 * uses {@link org.hibernate.sql.results.graph.DomainResultCreationState#visitNestedFetches(FetchParent)}
 * for creating the fetches for the attributes of the embeddable.
 */
public class AggregateEmbeddableResultImpl<T> extends AbstractFetchParent
		implements AggregateEmbeddableResultGraphNode, DomainResult<T>, EmbeddableResult<T>,
					InitializerProducer<AggregateEmbeddableResultImpl<T>> {
	private final String resultVariable;
	private final boolean containsAnyNonScalars;
	private final EmbeddableMappingType fetchContainer;
	private final BasicFetch<?> discriminatorFetch;
	private final int[] aggregateValuesArrayPositions;

	public AggregateEmbeddableResultImpl(
			NavigablePath navigablePath,
			EmbeddableValuedModelPart embeddedPartDescriptor,
			String resultVariable,
			DomainResultCreationState creationState) {
		/*
			An `{embeddable_result}` sub-path is created for the corresponding initializer to differentiate it from a fetch-initializer if this embedded is also fetched.
			The Jakarta Persistence spec says that any embedded value selected in the result should not be part of the state of any managed entity.
			Using this `{embeddable_result}` sub-path avoids this situation.
		*/
		super( navigablePath.append( "{embeddable_result}" ) );
		this.fetchContainer = embeddedPartDescriptor.getEmbeddableTypeDescriptor();
		this.resultVariable = resultVariable;

		final var sqlAstCreationState = creationState.getSqlAstCreationState();
		final var fromClauseAccess = sqlAstCreationState.getFromClauseAccess();
		final TableGroup tableGroup = fromClauseAccess.resolveTableGroup(
				getNavigablePath(),
				np -> {
					final EmbeddableValuedModelPart embeddedValueMapping =
							embeddedPartDescriptor.getEmbeddableTypeDescriptor()
									.getEmbeddedValueMapping();
					final TableGroup tg =
							fromClauseAccess.findTableGroup( NullnessUtil.castNonNull( np.getParent() ).getParent() );
					final TableGroupJoin tableGroupJoin = embeddedValueMapping.createTableGroupJoin(
							np,
							tg,
							resultVariable,
							null,
							SqlAstJoinType.INNER,
							true,
							false,
							sqlAstCreationState
					);
					tg.addTableGroupJoin( tableGroupJoin );
					return tableGroupJoin.getJoinedGroup();
				}
		);

		final var sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final var tableReference = tableGroup.getPrimaryTableReference();
		final var selectableMapping = embeddedPartDescriptor.getEmbeddableTypeDescriptor().getAggregateMapping();
		final Expression expression = sqlExpressionResolver.resolveSqlExpression( tableReference, selectableMapping );
		final var typeConfiguration = sqlAstCreationState.getCreationContext().getTypeConfiguration();
		final SqlSelection aggregateSelection = sqlExpressionResolver.resolveSqlSelection(
				expression,
				// Using the Object[] type here, so that a different JDBC extractor is chosen
				typeConfiguration.getJavaTypeRegistry().resolveDescriptor( Object[].class ),
				null,
				typeConfiguration
		);
		this.discriminatorFetch = creationState.visitEmbeddableDiscriminatorFetch( this, true );
		this.aggregateValuesArrayPositions = determineAggregateValuesArrayPositions( null, aggregateSelection );
		resetFetches( creationState.visitNestedFetches( this ) );
		this.containsAnyNonScalars = determineIfContainedAnyScalars( getFetches() );
	}

	@Override
	public int[] getAggregateValuesArrayPositions() {
		return aggregateValuesArrayPositions;
	}

	private static boolean determineIfContainedAnyScalars(ImmutableFetchList fetches) {
		for ( Fetch fetch : fetches ) {
			if ( fetch.containsAnyNonScalarResults() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public boolean containsAnyNonScalarResults() {
		return containsAnyNonScalars;
	}

	@Override
	public EmbeddableMappingType getFetchContainer() {
		return this.fetchContainer;
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return getReferencedMappingType().getJavaType();
	}

	@Override
	public EmbeddableMappingType getReferencedMappingType() {
		return getFetchContainer();
	}

	@Override
	public EmbeddableValuedModelPart getReferencedMappingContainer() {
		return getFetchContainer().getEmbeddedValueMapping();
	}

	@Override
	public DomainResultAssembler<T> createResultAssembler(
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		//noinspection unchecked
		return new EmbeddableAssembler( creationState.resolveInitializer( this, parent, this ).asEmbeddableInitializer() );
	}

	@Override
	public Initializer<?> createInitializer(
			AggregateEmbeddableResultImpl<T> resultGraphNode,
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return resultGraphNode.createInitializer( parent, creationState );
	}

	@Override
	public Initializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return new AggregateEmbeddableInitializerImpl(
				this,
				discriminatorFetch,
				parent,
				creationState,
				true
		);
	}
}

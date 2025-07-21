/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.internal.util.NullnessUtil;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.InitializerProducer;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResult;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class EmbeddableResultImpl<T> extends AbstractFetchParent implements EmbeddableResultGraphNode,
		DomainResult<T>,
		EmbeddableResult<T>,
		InitializerProducer<EmbeddableResultImpl<T>> {
	private final String resultVariable;
	private final boolean containsAnyNonScalars;
	private final EmbeddableMappingType fetchContainer;
	private final BasicFetch<?> discriminatorFetch;
	private final @Nullable DomainResult<Boolean> nullIndicatorResult;

	public EmbeddableResultImpl(
			NavigablePath navigablePath,
			EmbeddableValuedModelPart modelPart,
			String resultVariable,
			DomainResultCreationState creationState) {
		/*
			An `{embeddable_result}` sub-path is created for the corresponding initializer to differentiate it from a fetch-initializer if this embedded is also fetched.
			The Jakarta Persistence spec says that any embedded value selected in the result should not be part of the state of any managed entity.
			Using this `{embeddable_result}` sub-path avoids this situation.
		*/
		super( navigablePath.append( "{embeddable_result}" ) );
		this.fetchContainer = modelPart.getEmbeddableTypeDescriptor();
		this.resultVariable = resultVariable;

		final FromClauseAccess fromClauseAccess = creationState.getSqlAstCreationState().getFromClauseAccess();

		final TableGroup embeddableTableGroup = fromClauseAccess.resolveTableGroup(
				getNavigablePath(),
				np -> {
					final EmbeddableValuedModelPart embeddedValueMapping = modelPart.getEmbeddableTypeDescriptor().getEmbeddedValueMapping();
					final TableGroup tableGroup = fromClauseAccess.findTableGroup( NullnessUtil.castNonNull( np.getParent() ).getParent() );
					final TableGroupJoin tableGroupJoin = embeddedValueMapping.createTableGroupJoin(
							np,
							tableGroup,
							resultVariable,
							null,
							SqlAstJoinType.INNER,
							true,
							false,
							creationState.getSqlAstCreationState()
					);
					tableGroup.addTableGroupJoin( tableGroupJoin );
					return tableGroupJoin.getJoinedGroup();
				}
		);

		this.discriminatorFetch = creationState.visitEmbeddableDiscriminatorFetch( this, false );
		if ( fetchContainer.getAggregateMapping() != null ) {
			final TableReference tableReference = embeddableTableGroup.resolveTableReference(
					fetchContainer.getAggregateMapping().getContainingTableExpression() );
			final Expression aggregateExpression = creationState.getSqlAstCreationState().getSqlExpressionResolver()
					.resolveSqlExpression( tableReference, fetchContainer.getAggregateMapping() );
			final BasicType<Boolean> booleanType = creationState.getSqlAstCreationState().getCreationContext()
					.getTypeConfiguration().getBasicTypeForJavaType( Boolean.class );
			this.nullIndicatorResult = new NullnessPredicate( aggregateExpression, false, booleanType )
					.createDomainResult( null, creationState );
		}
		else {
			this.nullIndicatorResult = null;
		}

		afterInitialize( this, creationState );

		// after-after-initialize :D
		containsAnyNonScalars = determineIfContainedAnyScalars( getFetches() );
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
			EmbeddableResultImpl<T> resultGraphNode,
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return resultGraphNode.createInitializer( parent, creationState );
	}

	@Override
	public Initializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return new EmbeddableInitializerImpl( this, discriminatorFetch, nullIndicatorResult, parent, creationState, true );
	}
}

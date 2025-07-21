/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.FetchTiming;
import org.hibernate.graph.spi.GraphHelper;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.InitializerProducer;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.type.BasicType;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 */
public class EmbeddableFetchImpl extends AbstractFetchParent
		implements EmbeddableResultGraphNode, Fetch, InitializerProducer<EmbeddableFetchImpl> {

	private final FetchParent fetchParent;
	private final FetchTiming fetchTiming;
	private final TableGroup tableGroup;
	private final boolean hasTableGroup;
	private final EmbeddableMappingType fetchContainer;
	private final BasicFetch<?> discriminatorFetch;
	private final @Nullable DomainResult<Boolean> nullIndicatorResult;

	public EmbeddableFetchImpl(
			NavigablePath navigablePath,
			EmbeddableValuedFetchable embeddedPartDescriptor,
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean hasTableGroup,
			DomainResultCreationState creationState) {
		super( navigablePath );
		this.fetchContainer = embeddedPartDescriptor.getEmbeddableTypeDescriptor();

		this.fetchParent = fetchParent;
		this.fetchTiming = fetchTiming;
		this.hasTableGroup = hasTableGroup;

		this.tableGroup = creationState.getSqlAstCreationState().getFromClauseAccess().resolveTableGroup(
				getNavigablePath(),
				np -> {
					final TableGroup lhsTableGroup = creationState.getSqlAstCreationState()
							.getFromClauseAccess()
							.findTableGroup( fetchParent.getNavigablePath() );
					final TableGroupJoin tableGroupJoin = getReferencedMappingContainer().createTableGroupJoin(
							getNavigablePath(),
							lhsTableGroup,
							null,
							null,
							SqlAstJoinType.INNER,
							true,
							false,
							creationState.getSqlAstCreationState()
					);
					lhsTableGroup.addTableGroupJoin( tableGroupJoin );
					return tableGroupJoin.getJoinedGroup();
				}
		);

		this.discriminatorFetch = creationState.visitEmbeddableDiscriminatorFetch( this, false );
		if ( fetchContainer.getAggregateMapping() != null ) {
			final TableReference tableReference = tableGroup.resolveTableReference(
					fetchContainer.getAggregateMapping().getContainingTableExpression() );
			final Expression aggregateExpression = creationState.getSqlAstCreationState().getSqlExpressionResolver()
					.resolveSqlExpression( tableReference, fetchContainer.getAggregateMapping() );
			final BasicType<Boolean> booleanType = creationState.getSqlAstCreationState().getCreationContext()
					.getSessionFactory().getTypeConfiguration().getBasicTypeForJavaType( Boolean.class );
			this.nullIndicatorResult = new NullnessPredicate( aggregateExpression, false, booleanType )
					.createDomainResult( null, creationState );
		}
		else {
			this.nullIndicatorResult = null;
		}

		afterInitialize( this, creationState );
	}

	/**
	 * For Hibernate Reactive
	 */
	protected EmbeddableFetchImpl(EmbeddableFetchImpl original) {
		super( original );
		fetchContainer = original.getFetchContainer();
		fetchParent = original.fetchParent;
		fetchTiming = original.fetchTiming;
		tableGroup = original.tableGroup;
		hasTableGroup = original.hasTableGroup;
		discriminatorFetch = original.discriminatorFetch;
		nullIndicatorResult = original.nullIndicatorResult;
	}

	@Override
	public FetchTiming getTiming() {
		return fetchTiming;
	}

	@Override
	public boolean hasTableGroup() {
		return hasTableGroup;
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public EmbeddableMappingType getFetchContainer() {
		return this.fetchContainer;
	}

	@Override
	public EmbeddableValuedModelPart getReferencedMappingContainer() {
		return getFetchContainer().getEmbeddedValueMapping();
	}

	@Override
	public Fetchable getFetchedMapping() {
		return getReferencedMappingContainer();
	}


	@Override
	public NavigablePath resolveNavigablePath(Fetchable fetchable) {
		if ( fetchable instanceof TableGroupProducer ) {
			for ( TableGroupJoin tableGroupJoin : tableGroup.getTableGroupJoins() ) {
				final NavigablePath navigablePath = tableGroupJoin.getNavigablePath();
				if ( tableGroupJoin.getJoinedGroup().isFetched()
						&& fetchable.getFetchableName().equals( navigablePath.getLocalName() )
						&& tableGroupJoin.getJoinedGroup().getModelPart() == fetchable
						&& castNonNull( navigablePath.getParent() ).equals( getNavigablePath() ) ) {
					return navigablePath;
				}
			}
		}
		return super.resolveNavigablePath( fetchable );
	}

	@Override
	public EmbeddableMappingType getReferencedMappingType() {
		return getFetchContainer();
	}

	@Override
	public DomainResultAssembler<?> createAssembler(
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		Initializer<?> initializer = creationState.resolveInitializer( this, parent, this );
		EmbeddableInitializer<?> embeddableInitializer = initializer.asEmbeddableInitializer();
		return new EmbeddableAssembler( embeddableInitializer );
	}

	@Override
	public Initializer<?> createInitializer(
			EmbeddableFetchImpl resultGraphNode,
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return resultGraphNode.createInitializer( parent, creationState );
	}

	@Override
	public EmbeddableInitializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return new EmbeddableInitializerImpl( this, discriminatorFetch, nullIndicatorResult, parent, creationState, true );
	}

	@Override
	public boolean appliesTo(GraphImplementor<?> graphImplementor, JpaMetamodel metamodel) {
		// We use managedType here since this fetch could correspond to an entity type if the embeddable is an id-class
		return GraphHelper.appliesTo( graphImplementor, metamodel.managedType( getResultJavaType().getTypeName() ) );
	}

	@Override
	public FetchParent asFetchParent() {
		return this;
	}

	protected BasicFetch<?> getDiscriminatorFetch() {
		return discriminatorFetch;
	}
}

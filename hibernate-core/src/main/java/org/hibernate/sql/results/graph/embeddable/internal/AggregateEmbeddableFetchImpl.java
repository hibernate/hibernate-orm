/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A Fetch for an embeddable that is mapped as aggregate e.g. STRUCT, JSON or XML.
 * This is only used when {@link EmbeddableMappingType#shouldSelectAggregateMapping()} returns <code>true</code>.
 * The main difference is that it selects only the aggregate column and
 * uses {@link org.hibernate.sql.results.graph.DomainResultCreationState#visitNestedFetches(FetchParent)}
 * for creating the fetches for the attributes of the embeddable.
 */
public class AggregateEmbeddableFetchImpl extends AbstractFetchParent implements EmbeddableResultGraphNode, Fetch {
	private final FetchParent fetchParent;
	private final FetchTiming fetchTiming;
	private final TableGroup tableGroup;
	private final boolean hasTableGroup;
	private final SqlSelection aggregateSelection;

	public AggregateEmbeddableFetchImpl(
			NavigablePath navigablePath,
			EmbeddableValuedFetchable embeddedPartDescriptor,
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean hasTableGroup,
			DomainResultCreationState creationState) {
		super( embeddedPartDescriptor.getEmbeddableTypeDescriptor(), navigablePath );

		this.fetchParent = fetchParent;
		this.fetchTiming = fetchTiming;
		this.hasTableGroup = hasTableGroup;

		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		this.tableGroup = sqlAstCreationState.getFromClauseAccess().resolveTableGroup(
				getNavigablePath(),
				np -> {
					final TableGroup lhsTableGroup = sqlAstCreationState.getFromClauseAccess()
							.findTableGroup( fetchParent.getNavigablePath() );
					final TableGroupJoin tableGroupJoin = getReferencedMappingContainer().createTableGroupJoin(
							getNavigablePath(),
							lhsTableGroup,
							null,
							null,
							SqlAstJoinType.INNER,
							true,
							false,
							sqlAstCreationState
					);
					lhsTableGroup.addTableGroupJoin( tableGroupJoin );
					return tableGroupJoin.getJoinedGroup();
				}

		);

		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final TableReference tableReference = tableGroup.getPrimaryTableReference();
		final SelectableMapping selectableMapping = embeddedPartDescriptor.getEmbeddableTypeDescriptor().getAggregateMapping();
		final Expression expression = sqlExpressionResolver.resolveSqlExpression( tableReference, selectableMapping );
		final TypeConfiguration typeConfiguration = sqlAstCreationState.getCreationContext()
				.getSessionFactory()
				.getTypeConfiguration();
		this.aggregateSelection = sqlExpressionResolver.resolveSqlSelection(
				expression,
				typeConfiguration.getJavaTypeRegistry().resolveDescriptor( Object[].class ),
				fetchParent,
				typeConfiguration
		);
		resetFetches( creationState.visitNestedFetches( this ) );
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
		return (EmbeddableMappingType) super.getFetchContainer();
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
						&& tableGroupJoin.getJoinedGroup().getModelPart() == fetchable ) {
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
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		final EmbeddableInitializer initializer = creationState.resolveInitializer(
				getNavigablePath(),
				getReferencedModePart(),
				() -> new AggregateEmbeddableFetchInitializer(
						parentAccess,
						this,
						creationState,
						aggregateSelection
				)
		).asEmbeddableInitializer();

		assert initializer != null;

		return new EmbeddableAssembler( initializer );
	}
}

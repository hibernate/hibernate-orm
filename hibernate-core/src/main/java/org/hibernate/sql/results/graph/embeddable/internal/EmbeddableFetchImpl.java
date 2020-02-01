/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;

/**
 * @author Steve Ebersole
 */
public class EmbeddableFetchImpl extends AbstractFetchParent implements EmbeddableResultGraphNode, Fetch {
	private final FetchParent fetchParent;
	private final FetchTiming fetchTiming;
	private final boolean hasTableGroup;
	private final boolean nullable;

	public EmbeddableFetchImpl(
			NavigablePath navigablePath,
			EmbeddableValuedFetchable embeddedPartDescriptor,
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean hasTableGroup,
			boolean nullable,
			DomainResultCreationState creationState) {
		super( embeddedPartDescriptor.getEmbeddableTypeDescriptor(), navigablePath );

		this.fetchParent = fetchParent;
		this.fetchTiming = fetchTiming;
		this.hasTableGroup = hasTableGroup;
		this.nullable = nullable;

		creationState.getSqlAstCreationState().getFromClauseAccess().resolveTableGroup(
				getNavigablePath(),
				np -> {
					final TableGroupJoin tableGroupJoin = getReferencedMappingContainer().createTableGroupJoin(
							getNavigablePath(),
							creationState.getSqlAstCreationState()
									.getFromClauseAccess()
									.findTableGroup( fetchParent.getNavigablePath() ),
							null,
							nullable ? SqlAstJoinType.LEFT : SqlAstJoinType.INNER,
							LockMode.NONE,
							stem -> creationState.getSqlAliasBaseManager().createSqlAliasBase( stem ),
							creationState.getSqlAstCreationState().getSqlExpressionResolver(),
							creationState.getSqlAstCreationState().getCreationContext()
					);

					return tableGroupJoin.getJoinedGroup();
				}

		);

		afterInitialize( creationState );
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
	public EmbeddableMappingType getReferencedMappingType() {
		return getFetchContainer();
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {
		final EmbeddableFetchInitializer initializer = new EmbeddableFetchInitializer(
				parentAccess,
				this,
				collector,
				creationState
		);


		collector.accept( initializer );

		return new EmbeddableAssembler( initializer );
	}
}

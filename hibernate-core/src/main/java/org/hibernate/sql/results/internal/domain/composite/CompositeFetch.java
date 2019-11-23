/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.composite;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.results.internal.domain.AbstractFetchParent;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CompositeResultNode;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Fetchable;
import org.hibernate.sql.results.spi.Initializer;

/**
 * @author Steve Ebersole
 */
public class CompositeFetch extends AbstractFetchParent implements CompositeResultNode, Fetch {
	private final FetchParent fetchParent;
	private final FetchTiming fetchTiming;
	private final boolean nullable;

	public CompositeFetch(
			NavigablePath navigablePath,
			EmbeddableValuedModelPart embeddedPartDescriptor,
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean nullable,
			DomainResultCreationState creationState) {
		super( embeddedPartDescriptor.getEmbeddableTypeDescriptor(), navigablePath );

		this.fetchParent = fetchParent;
		this.fetchTiming = fetchTiming;
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
							nullable ? JoinType.LEFT : JoinType.INNER,
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
		final CompositeFetchInitializer initializer = new CompositeFetchInitializer(
				parentAccess,
				this,
				collector,
				creationState
		);


		collector.accept( initializer );

		return new CompositeAssembler( initializer );
	}
}

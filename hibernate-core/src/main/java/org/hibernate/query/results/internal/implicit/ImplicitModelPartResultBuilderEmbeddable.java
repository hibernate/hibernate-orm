/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.implicit;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.ResultBuilderEmbeddable;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.query.results.internal.ResultsHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Steve Ebersole
 */
public class ImplicitModelPartResultBuilderEmbeddable
		implements ImplicitModelPartResultBuilder, ResultBuilderEmbeddable {

	private final NavigablePath navigablePath;
	private final EmbeddableValuedModelPart modelPart;

	public ImplicitModelPartResultBuilderEmbeddable(
			NavigablePath navigablePath,
			EmbeddableValuedModelPart modelPart) {
		this.navigablePath = navigablePath;
		this.modelPart = modelPart;
	}

	@Override
	public Class<?> getJavaType() {
		return modelPart.getJavaType().getJavaTypeClass();
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public EmbeddableResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = ResultsHelper.impl( domainResultCreationState );
		creationStateImpl.disallowPositionalSelections();
		return (EmbeddableResult<?>) modelPart.createDomainResult(
				navigablePath,
				tableGroup( creationStateImpl ),
				null,
				domainResultCreationState
		);

	}

	private TableGroup tableGroup(DomainResultCreationStateImpl creationStateImpl) {
		return creationStateImpl.getFromClauseAccess().resolveTableGroup(
				navigablePath,
				np -> {
					if ( navigablePath.getParent() == null ) {
						throw new IllegalStateException(
								"Could not determine LHS for implicit embeddable result builder - " + navigablePath
						);
					}

					final TableGroup parentTableGroup =
							creationStateImpl.getFromClauseAccess()
									.getTableGroup( navigablePath.getParent() );

					final TableGroupJoin tableGroupJoin = modelPart.createTableGroupJoin(
							navigablePath,
							parentTableGroup,
							null,
							null,
							SqlAstJoinType.INNER,
							true,
							false,
							creationStateImpl
					);

					parentTableGroup.addTableGroupJoin( tableGroupJoin );

					return tableGroupJoin.getJoinedGroup();
				}
		);
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final ImplicitModelPartResultBuilderEmbeddable that = (ImplicitModelPartResultBuilderEmbeddable) o;
		return navigablePath.equals( that.navigablePath )
				&& modelPart.equals( that.modelPart );
	}

	@Override
	public int hashCode() {
		int result = navigablePath.hashCode();
		result = 31 * result + modelPart.hashCode();
		return result;
	}
}

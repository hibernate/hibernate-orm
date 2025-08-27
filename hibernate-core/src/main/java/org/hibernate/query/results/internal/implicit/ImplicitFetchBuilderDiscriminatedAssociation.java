/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.implicit;


import org.hibernate.metamodel.mapping.internal.DiscriminatedAssociationAttributeMapping;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.internal.ResultsHelper.impl;

public class ImplicitFetchBuilderDiscriminatedAssociation implements ImplicitFetchBuilder {
	private final NavigablePath fetchPath;
	private final DiscriminatedAssociationAttributeMapping fetchable;

	public ImplicitFetchBuilderDiscriminatedAssociation(
			NavigablePath fetchPath,
			DiscriminatedAssociationAttributeMapping fetchable,
			DomainResultCreationState creationState) {
		this.fetchPath = fetchPath;
		this.fetchable = fetchable;
	}

	@Override
	public FetchBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState creationState) {
		tableGroup( parent, fetchPath, impl( creationState ) );
		return parent.generateFetchableFetch(
				fetchable,
				fetchPath,
				fetchable.getTiming(),
				false,
				null,
				creationState
		);
	}

	private void tableGroup(FetchParent parent, NavigablePath fetchPath, DomainResultCreationStateImpl creationStateImpl) {
		creationStateImpl.getFromClauseAccess().resolveTableGroup(
				fetchPath,
				navigablePath -> {
					final TableGroup parentTableGroup =
							creationStateImpl.getFromClauseAccess()
									.getTableGroup( parent.getNavigablePath() );
					final TableGroupJoin tableGroupJoin = fetchable.createTableGroupJoin(
							fetchPath,
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

		final ImplicitFetchBuilderDiscriminatedAssociation that = (ImplicitFetchBuilderDiscriminatedAssociation) o;
		return fetchPath.equals( that.fetchPath )
			&& fetchable.equals( that.fetchable );
	}

	@Override
	public int hashCode() {
		int result = fetchPath.hashCode();
		result = 31 * result + fetchable.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "ImplicitFetchBuilderDiscriminatedAssociation(" + fetchPath + ")";
	}

}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.dynamic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Steve Ebersole
 */
public class DynamicFetchBuilderStandard
		implements DynamicFetchBuilder, NativeQuery.ReturnProperty {

	private final DynamicFetchBuilderContainer container;
	private final String fetchableName;

	private final List<String> columnNames = new ArrayList<>();

	public DynamicFetchBuilderStandard(
			DynamicFetchBuilderContainer container,
			String fetchableName) {
		this.container = container;
		this.fetchableName = fetchableName;
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = ResultsHelper.impl( domainResultCreationState );

		final TableGroup ownerTableGroup = creationStateImpl.getFromClauseAccess().getTableGroup( parent.getNavigablePath() );

		// todo (6.0) : create the TableGroupJoin for the fetch and then build the fetch

		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public NativeQuery.ReturnProperty addColumnAlias(String columnAlias) {
		columnNames.add( columnAlias );
		return this;
	}
}

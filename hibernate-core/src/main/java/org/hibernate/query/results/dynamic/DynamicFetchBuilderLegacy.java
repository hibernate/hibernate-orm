/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.dynamic;

import java.util.List;
import java.util.function.BiFunction;

import org.hibernate.LockMode;
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
public class DynamicFetchBuilderLegacy implements DynamicFetchBuilder, NativeQuery.FetchReturn {

	private final String tableAlias;

	private final String ownerTableAlias;
	private final String fetchableName;

	private final List<String> columnNames;

	public DynamicFetchBuilderLegacy(
			String tableAlias,
			String ownerTableAlias,
			String fetchableName,
			List<String> columnNames) {
		this.tableAlias = tableAlias;
		this.ownerTableAlias = ownerTableAlias;
		this.fetchableName = fetchableName;
		this.columnNames = columnNames;
	}

	public String getTableAlias() {
		return tableAlias;
	}

	public String getOwnerAlias() {
		return ownerTableAlias;
	}

	public String getFetchableName() {
		return fetchableName;
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationState = ResultsHelper.impl( domainResultCreationState );

		final TableGroup ownerTableGroup = creationState.getFromClauseAccess().findByAlias( ownerTableAlias );

		// todo (6.0) : create the TableGroupJoin for the fetch and then build the fetch

		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public NativeQuery.ReturnProperty addColumnAlias(String columnAlias) {
		columnNames.add( columnAlias );
		return this;
	}

	@Override
	public NativeQuery.FetchReturn setLockMode(LockMode lockMode) {
		return null;
	}

	@Override
	public NativeQuery.FetchReturn addProperty(String propertyName, String columnAlias) {
		return null;
	}

	@Override
	public NativeQuery.ReturnProperty addProperty(String propertyName) {
		return null;
	}
}

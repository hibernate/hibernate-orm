/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.internal;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.results.spi.LegacyFetchBuilder;
import org.hibernate.query.results.spi.ResultBuilder;
import org.hibernate.query.results.spi.ResultSetMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/// ResultSetMapping implementation for situations where we *know*
/// the native-query is a mutation.
///
/// @author Steve Ebersole
public class MutationResultSetMapping implements ResultSetMapping {
	public static final MutationResultSetMapping INSTANCE = new MutationResultSetMapping();

	@Override
	public String getMappingIdentifier() {
		return "";
	}

	@Override
	public boolean isDynamic() {
		return false;
	}

	@Override
	public int getNumberOfResultBuilders() {
		return 0;
	}

	@Override
	public List<ResultBuilder> getResultBuilders() {
		return List.of();
	}

	@Override
	public void visitResultBuilders(BiConsumer<Integer, ResultBuilder> resultBuilderConsumer) {
	}

	@Override
	public void visitLegacyFetchBuilders(Consumer<LegacyFetchBuilder> resultBuilderConsumer) {
	}

	@Override
	public void addResultBuilder(ResultBuilder resultBuilder) {
		throw new UnsupportedOperationException( "MutationQuery cannot define results" );
	}

	@Override
	public void addLegacyFetchBuilder(LegacyFetchBuilder fetchBuilder) {
		throw new UnsupportedOperationException( "MutationQuery cannot define results" );
	}

	@Override
	public NamedResultSetMappingMemento toMemento(String name) {
		throw new UnsupportedOperationException( "MutationQuery cannot define results" );
	}

	@Override
	public JdbcValuesMapping resolve(JdbcValuesMetadata jdbcResultsMetadata, LoadQueryInfluencers loadQueryInfluencers, SessionFactoryImplementor sessionFactory) {
		throw new UnsupportedOperationException( "MutationQuery cannot define results" );
	}

	@Override
	public void addAffectedTableNames(Set<String> affectedTableNames, SessionFactoryImplementor sessionFactory) {
		throw new UnsupportedOperationException( "MutationQuery cannot define results" );
	}

	@Override
	public ResultSetMapping cacheKeyInstance() {
		return null;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.implicit;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.query.results.spi.FetchBuilder;
import org.hibernate.query.results.spi.FetchBuilderBasicValued;
import org.hibernate.query.results.internal.ResultsHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import java.util.function.BiConsumer;

import static org.hibernate.query.results.internal.ResultsHelper.impl;
import static org.hibernate.query.results.internal.ResultsHelper.resolveSqlExpression;

/**
 * @author Steve Ebersole
 */
public class ImplicitFetchBuilderBasic implements ImplicitFetchBuilder, FetchBuilderBasicValued {
	private final NavigablePath fetchPath;
	private final BasicValuedModelPart fetchable;
	private final FetchBuilder fetchBuilder;

	public ImplicitFetchBuilderBasic(NavigablePath fetchPath, BasicValuedModelPart fetchable) {
		this.fetchPath = fetchPath;
		this.fetchable = fetchable;
		this.fetchBuilder = null;
	}

	public ImplicitFetchBuilderBasic(
			NavigablePath fetchPath,
			BasicValuedModelPart fetchable,
			DomainResultCreationState creationState) {
		this.fetchPath = fetchPath;
		this.fetchable = fetchable;
		this.fetchBuilder = impl( creationState ).getCurrentExplicitFetchMementoResolver().apply( fetchable );
	}

	@Override
	public FetchBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public BasicFetch<?> buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState) {
		if ( fetchBuilder != null ) {
			return (BasicFetch<?>) fetchBuilder.buildFetch(
					parent,
					fetchPath,
					jdbcResultsMetadata,
					domainResultCreationState
			);
		}

		final var sqlSelection =
				sqlSelection( parent, fetchPath, jdbcResultsMetadata, domainResultCreationState );
		return new BasicFetch<>(
				sqlSelection.getValuesArrayPosition(),
				parent,
				fetchPath,
				fetchable,
				FetchTiming.IMMEDIATE,
				!sqlSelection.isVirtual()
		);
	}

	private SqlSelection sqlSelection(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState) {
		final var creationStateImpl = ResultsHelper.impl( domainResultCreationState );
		final var parentTableGroup =
				creationStateImpl.getFromClauseAccess()
						.getTableGroup( parent.getNavigablePath() );
		return creationStateImpl.resolveSqlSelection(
				resolveSqlExpression(
						creationStateImpl,
						jdbcResultsMetadata,
						parentTableGroup.resolveTableReference( fetchPath, fetchable,
								fetchable.getContainingTableExpression() ),
						fetchable,
						fetchable.isFormula()
								// In case of a formula we look for a result set position with the fetchable name
								? fetchable.getFetchableName()
								: fetchable.getSelectionExpression()
				),
				fetchable.getJdbcMapping().getJdbcJavaType(),
				parent,
				domainResultCreationState.getSqlAstCreationState().getCreationContext()
						.getTypeConfiguration()
		);
	}

	@Override
	public String toString() {
		return "ImplicitFetchBuilderBasic(" + fetchPath + ")";
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !( object instanceof ImplicitFetchBuilderBasic that ) ) {
			return false;
		}
		else {
			return fetchPath.equals( that.fetchPath )
				&& fetchable.equals( that.fetchable );
		}
	}

	@Override
	public int hashCode() {
		int result = fetchPath.hashCode();
		result = 31 * result + fetchable.hashCode();
		return result;
	}

	@Override
	public void visitFetchBuilders(BiConsumer<Fetchable, FetchBuilder> consumer) {
		if ( fetchBuilder != null ) {
			consumer.accept( fetchable, fetchBuilder );
		}
	}
}

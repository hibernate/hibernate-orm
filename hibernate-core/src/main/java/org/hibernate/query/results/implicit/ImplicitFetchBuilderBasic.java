/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.implicit;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.results.BasicValuedFetchBuilder;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.ResultsHelper.impl;
import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Steve Ebersole
 */
public class ImplicitFetchBuilderBasic implements ImplicitFetchBuilder, BasicValuedFetchBuilder {
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
		final DomainResultCreationStateImpl creationStateImpl = impl( creationState );
		final Function<String, FetchBuilder> fetchBuilderResolver = creationStateImpl.getCurrentExplicitFetchMementoResolver();
		this.fetchBuilder = fetchBuilderResolver.apply( fetchable.getFetchableName() );
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
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		if ( fetchBuilder != null ) {
			return (BasicFetch<?>) fetchBuilder.buildFetch(
					parent,
					fetchPath,
					jdbcResultsMetadata,
					legacyFetchResolver,
					domainResultCreationState
			);
		}
		final DomainResultCreationStateImpl creationStateImpl = ResultsHelper.impl( domainResultCreationState );

		final TableGroup parentTableGroup = creationStateImpl
				.getFromClauseAccess()
				.getTableGroup( parent.getNavigablePath() );

		final String table = fetchable.getContainingTableExpression();
		final String column;

		// In case of a formula we look for a result set position with the fetchable name
		if ( fetchable.isFormula() ) {
			column = fetchable.getFetchableName();
		}
		else {
			column = fetchable.getSelectionExpression();
		}

		final Expression expression = ResultsHelper.resolveSqlExpression(
				creationStateImpl,
				jdbcResultsMetadata,
				parentTableGroup.resolveTableReference( fetchPath, fetchable, table ),
				fetchable,
				column
		);

		final SqlSelection sqlSelection = creationStateImpl.resolveSqlSelection(
				expression,
				fetchable.getJdbcMapping().getJdbcJavaType(),
				parent,
				domainResultCreationState.getSqlAstCreationState()
						.getCreationContext()
						.getSessionFactory()
						.getTypeConfiguration()
		);

		return new BasicFetch<>(
				sqlSelection.getValuesArrayPosition(),
				parent,
				fetchPath,
				fetchable,
				FetchTiming.IMMEDIATE,
				domainResultCreationState,
				!sqlSelection.isVirtual()
		);
	}

	@Override
	public String toString() {
		return "ImplicitFetchBuilderBasic(" + fetchPath + ")";
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final ImplicitFetchBuilderBasic that = (ImplicitFetchBuilderBasic) o;
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
	public void visitFetchBuilders(BiConsumer<String, FetchBuilder> consumer) {
		if ( fetchBuilder != null ) {
			consumer.accept( fetchPath.getLocalName(), fetchBuilder );
		}
	}
}

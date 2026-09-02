/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal.lock;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.results.internal.FromClauseAccessImpl;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.creation.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.creation.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Used by {@linkplain TableLock} in creating the SQL AST and DomainResults
 * as part of its follow-on lock handling.
 *
 * @author Steve Ebersole
 */
public class LockingCreationStates
		implements DomainResultCreationState, SqlAstCreationState, SqlAstProcessingState, SqlExpressionResolver {

	private final QuerySpec querySpec;
	private final SessionFactoryImplementor sessionFactory;

	private final FromClauseAccessImpl fromClauseAccess;
	private final SqlAliasBaseManager sqlAliasBaseManager;

	private final Map<ColumnReferenceKey, Expression> sqlExpressionMap = new HashMap<>();
	private final Map<Expression, SqlSelection> sqlSelectionMap = new HashMap<>();

	public LockingCreationStates(
			QuerySpec querySpec,
			TableGroup root,
			SessionFactoryImplementor sessionFactory) {
		this.querySpec = querySpec;
		this.sessionFactory = sessionFactory;

		fromClauseAccess = new FromClauseAccessImpl();
		fromClauseAccess.registerTableGroup( root.getNavigablePath(), root );

		sqlAliasBaseManager = new SqlAliasBaseManager();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultCreationState

	@Override
	public FromClauseAccessImpl getFromClauseAccess() {
		return fromClauseAccess;
	}

	@Override
	public SqlAliasBaseManager getSqlAliasBaseManager() {
		return sqlAliasBaseManager;
	}

	@Override
	public LockingCreationStates getSqlAstCreationState() {
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlAstCreationState

	@Override
	public SqlAstCreationContext getCreationContext() {
		return sessionFactory.getSqlTranslationEngine();
	}

	@Override
	public LockingCreationStates getCurrentProcessingState() {
		return this;
	}

	@Override
	public LockingCreationStates getSqlExpressionResolver() {
		return getCurrentProcessingState();
	}

	@Override
	public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
		return sqlAliasBaseManager;
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return null;
	}

	@Override
	public boolean applyOnlyLoadByKeyFilters() {
		return true;
	}

	@Override
	public void registerLockMode(String identificationVariable, LockMode explicitLockMode) {
		throw new UnsupportedOperationException();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlAstProcessingState

	@Override
	public SqlAstProcessingState getParentState() {
		return null;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlExpressionResolver

	@Override
	public Expression resolveSqlExpression(
			ColumnReferenceKey key,
			Function<SqlAstProcessingState, Expression> creator) {
		final Expression expression = sqlExpressionMap.get( key );
		if ( expression != null ) {
			return expression;
		}

		final Expression created = creator.apply( this );
		sqlExpressionMap.put( key, created );
		return created;
	}

	@Override
	public SqlSelection resolveSqlSelection(
			Expression expression,
			JavaType<?> javaType,
			FetchParent fetchParent,
			TypeConfiguration typeConfiguration) {
		final SqlSelection sqlSelection = sqlSelectionMap.get( expression );
		if ( sqlSelection != null ) {
			return sqlSelection;
		}

		if ( expression instanceof ColumnReference columnReference ) {
			final var selection =
					new SqlSelectionImpl( columnReference, querySpec.getSelectClause().getSqlSelections().size() );
			sqlSelectionMap.put( expression, selection );
			querySpec.getSelectClause().addSqlSelection( selection );
			return selection;
		}

		throw new UnsupportedOperationException( "Unsupported Expression type (expected ColumnReference) : " + expression );
	}

	@Override
	public ModelPart resolveModelPart(NavigablePath navigablePath) {
		return null;
	}

	@Override
	public ImmutableFetchList visitFetches(FetchParent fetchParent) {
		final var fetches = new ImmutableFetchList.Builder( fetchParent.getReferencedMappingContainer() );
		final var referencedMappingContainer = fetchParent.getReferencedMappingContainer();
		final int size = referencedMappingContainer.getNumberOfFetchables();
		for ( int i = 0; i < size; i++ ) {
			final Fetchable fetchable = referencedMappingContainer.getFetchable( i );
			processFetchable( fetchParent, fetchable, fetches );
		}
		return fetches.build();
	}

	private void processFetchable(FetchParent fetchParent, Fetchable fetchable, ImmutableFetchList.Builder fetches) {
		if ( fetchable.isSelectable() ) {
			fetches.add( fetchParent.generateFetchableFetch(
					fetchable,
					fetchParent.resolveNavigablePath( fetchable ),
					FetchTiming.DELAYED,
					false,
					null,
					this
			) );
		}
	}

	@Override
	public <R> R withNestedFetchParent(FetchParent fetchParent, Function<FetchParent, R> action) {
		return null;
	}

	@Override
	public boolean isResolvingCircularFetch() {
		return false;
	}

	@Override
	public void setResolvingCircularFetch(boolean resolvingCircularFetch) {
	}

	@Override
	public ForeignKeyDescriptor.Nature getCurrentlyResolvingForeignKeyPart() {
		return null;
	}

	@Override
	public void setCurrentlyResolvingForeignKeyPart(ForeignKeyDescriptor.Nature currentlyResolvingForeignKeySide) {
	}
}

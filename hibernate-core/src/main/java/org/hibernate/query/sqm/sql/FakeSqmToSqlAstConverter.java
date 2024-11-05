/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.spi.BaseSemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryTransformer;
import org.hibernate.sql.ast.tree.predicate.Predicate;

import jakarta.annotation.Nullable;

/**
 *
 */
public class FakeSqmToSqlAstConverter extends BaseSemanticQueryWalker implements SqmToSqlAstConverter {

	private final SqlAstCreationState creationState;

	public FakeSqmToSqlAstConverter(SqlAstCreationState creationState) {
		this.creationState = creationState;
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlAstCreationState

	@Override
	public SqlAstCreationContext getCreationContext() {
		return creationState.getCreationContext();
	}

	@Override
	public SqlAstProcessingState getCurrentProcessingState() {
		return creationState.getCurrentProcessingState();
	}

	@Override
	public SqlExpressionResolver getSqlExpressionResolver() {
		return creationState.getSqlExpressionResolver();
	}

	@Override
	public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
		return creationState.getSqlAliasBaseGenerator();
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return new LoadQueryInfluencers( getCreationContext().getSessionFactory() );
	}

	@Override
	public boolean applyOnlyLoadByKeyFilters() {
		return false;
	}

	@Override
	public void registerLockMode(String identificationVariable, LockMode explicitLockMode) {
		creationState.registerLockMode( identificationVariable, explicitLockMode );
	}

	@Override
	public FromClauseAccess getFromClauseAccess() {
		return creationState.getFromClauseAccess();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqmToSqlAstConverter

	@Override
	public Stack<Clause> getCurrentClauseStack() {
		return null;
	}

	@Override
	public Stack<SqmQueryPart> getSqmQueryPartStack() {
		return null;
	}

	@Override
	public SqmQueryPart<?> getCurrentSqmQueryPart() {
		return null;
	}

	@Override
	public void registerQueryTransformer(QueryTransformer transformer) {
	}

	@Override
	public @Nullable SqlAstJoinType getCurrentlyProcessingJoinType() {
		return null;
	}

	@Override
	public boolean isInTypeInference() {
		return false;
	}

	@Override
	public MappingModelExpressible<?> resolveFunctionImpliedReturnType() {
		return null;
	}

	@Override
	public MappingModelExpressible<?> determineValueMapping(SqmExpression<?> sqmExpression) {
		return null;
	}

	@Override
	public Object visitWithInferredType(
			SqmVisitableNode node,
			Supplier<MappingModelExpressible<?>> inferredTypeAccess) {
		return node.accept( this );
	}

	@Override
	public List<Expression> expandSelfRenderingFunctionMultiValueParameter(SqmParameter<?> sqmParameter) {
		return null;
	}

	@Override
	public Predicate visitNestedTopLevelPredicate(SqmPredicate predicate) {
		return (Predicate) predicate.accept( this );
	}
}

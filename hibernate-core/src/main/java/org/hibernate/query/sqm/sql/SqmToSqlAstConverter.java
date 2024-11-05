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

import org.hibernate.internal.util.collections.Stack;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryTransformer;
import org.hibernate.sql.ast.tree.predicate.Predicate;

import jakarta.annotation.Nullable;

/**
 * Specialized SemanticQueryWalker (SQM visitor) for producing SQL AST.
 *
 * @author Steve Ebersole
 */
public interface SqmToSqlAstConverter extends SemanticQueryWalker<Object>, SqlAstCreationState {
	Stack<Clause> getCurrentClauseStack();

	Stack<SqmQueryPart> getSqmQueryPartStack();

	default SqmQueryPart<?> getCurrentSqmQueryPart() {
		return getSqmQueryPartStack().getCurrent();
	}

	void registerQueryTransformer(QueryTransformer transformer);

	/**
	 * Returns the {@link SqlAstJoinType} of the currently processing join if there is one, or {@code null}.
	 * This is used to determine the join type for implicit joins happening in the {@code ON} clause.
	 */
	@Nullable SqlAstJoinType getCurrentlyProcessingJoinType();

	/**
	 * Returns whether the state of the translation is currently in type inference mode.
	 * This is useful to avoid type inference based on other incomplete inference information.
	 */
	boolean isInTypeInference();

	/**
	 * Returns the function return type implied from the context within which it is used.
	 * If there is no current function being processed or no context implied type, the return is <code>null</code>.
	 */
	MappingModelExpressible<?> resolveFunctionImpliedReturnType();

	MappingModelExpressible<?> determineValueMapping(SqmExpression<?> sqmExpression);

	/**
	 * Visits the given node with the given inferred type access.
	 */
	Object visitWithInferredType(SqmVisitableNode node, Supplier<MappingModelExpressible<?>> inferredTypeAccess);

	List<Expression> expandSelfRenderingFunctionMultiValueParameter(SqmParameter<?> sqmParameter);

	Predicate visitNestedTopLevelPredicate(SqmPredicate predicate);

	/**
	 * Resolve a generic metadata object from the provided source, using the specified producer.
	 */
	default <S, M> M resolveMetadata(S source, Function<S, M> producer) {
		return producer.apply( source );
	}
}

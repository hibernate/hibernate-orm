/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;

/**
 * Support for {@link SqmFunctionDescriptor}s that ultimately want
 * to perform SQL rendering themselves. This is a protocol passed
 * from the {@link AbstractSqmSelfRenderingFunctionDescriptor}
 * along to its {@link SelfRenderingSqmFunction} and ultimately to
 * the {@link SelfRenderingFunctionSqlAstExpression} which calls it
 * to finally render SQL.
 *
 * @author Steve Ebersole
 * @since 6.4
 */
@FunctionalInterface
public interface FunctionRenderer extends FunctionRenderingSupport {
	/**
	 * @deprecated Use {@link #render(SqlAppender, List, ReturnableType, SqlAstTranslator)} instead
	 */
	@Deprecated(forRemoval = true)
	default void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, (ReturnableType<?>) null, walker );
	}

	void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker);

	default void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// Ignore the filter by default. Subclasses will override this
		render( sqlAppender, sqlAstArguments, returnType, walker );
	}

	default void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// Ignore the filter by default. Subclasses will override this
		render( sqlAppender, sqlAstArguments, returnType, walker );
	}

	default void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			Boolean respectNulls,
			Boolean fromFirst,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// Ignore the filter by default. Subclasses will override this
		render( sqlAppender, sqlAstArguments, returnType, walker );
	}

}

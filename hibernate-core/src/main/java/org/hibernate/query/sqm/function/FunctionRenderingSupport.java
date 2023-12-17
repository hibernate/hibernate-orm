/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SortSpecification;

import java.util.List;

/**
 * Support for {@link SqmFunctionDescriptor}s that ultimately want
 * to perform SQL rendering themselves. This is a protocol passed
 * from the {@link AbstractSqmSelfRenderingFunctionDescriptor}
 * along to its {@link SelfRenderingSqmFunction} and ultimately to
 * the {@link SelfRenderingFunctionSqlAstExpression} which calls it
 * to finally render SQL.
 *
 * @author Steve Ebersole
 * @deprecated Use {@link FunctionRenderer} instead
 */
@Deprecated(forRemoval = true)
@FunctionalInterface
public interface FunctionRenderingSupport {
	void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker);

	/**
	 * @deprecated Use {@link #render(SqlAppender, List, Predicate, ReturnableType, SqlAstTranslator)} instead
	 */
	@Deprecated(forRemoval = true)
	default void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			SqlAstTranslator<?> walker) {
		// Ignore the filter by default. Subclasses will override this
		render( sqlAppender, sqlAstArguments, walker );
	}

	/**
	 * @deprecated Use {@link #render(SqlAppender, List, Predicate, List, ReturnableType, SqlAstTranslator)} instead
	 */
	@Deprecated(forRemoval = true)
	default void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			SqlAstTranslator<?> walker) {
		// Ignore the filter by default. Subclasses will override this
		render( sqlAppender, sqlAstArguments, walker );
	}

	/**
	 * @deprecated Use {@link #render(SqlAppender, List, Predicate, Boolean, Boolean, ReturnableType, SqlAstTranslator)} instead
	 */
	@Deprecated(forRemoval = true)
	default void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			Boolean respectNulls,
			Boolean fromFirst,
			SqlAstTranslator<?> walker) {
		// Ignore the filter by default. Subclasses will override this
		render( sqlAppender, sqlAstArguments, walker );
	}

	/**
	 * @since 6.4
	 */
	default void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// Ignore the return type by default. Subclasses will override this
		render( sqlAppender, sqlAstArguments, walker );
	}

	/**
	 * @since 6.4
	 */
	default void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// Ignore the filter by default. Subclasses will override this
		render( sqlAppender, sqlAstArguments, returnType, walker );
	}

	/**
	 * @since 6.4
	 */
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

	/**
	 * @since 6.4
	 */
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

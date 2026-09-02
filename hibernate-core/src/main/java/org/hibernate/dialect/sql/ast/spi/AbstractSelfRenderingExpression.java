/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import jakarta.annotation.Nullable;
import org.hibernate.SPI;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Supported provider base for a SQL AST expression which renders itself.
///
/// The base retains the expression type and owns the SQL AST visitor dispatch.
/// A provider subclass implements only [#renderToSql]. Any state retained by a
/// subclass should be limited to the request-scoped expression being rendered.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT })
public abstract class AbstractSelfRenderingExpression implements SelfRenderingExpression {
	private final @Nullable JdbcMappingContainer expressionType;

	/// Creates an expression with the given possibly-null mapping type.
	///
	/// @since 8.0
	@SPI(IMPLEMENT)
	protected AbstractSelfRenderingExpression(@Nullable JdbcMappingContainer expressionType) {
		this.expressionType = expressionType;
	}

	/// Returns the same mapping-type reference supplied during construction.
	///
	/// @since 8.0
	@Override
	public final @Nullable JdbcMappingContainer getExpressionType() {
		return expressionType;
	}

	/// Dispatches this expression to
	/// [SqlAstWalker#visitSelfRenderingExpression].
	///
	/// @since 8.0
	@Override
	public final void accept(SqlAstWalker walker) {
		walker.visitSelfRenderingExpression( this );
	}

	/// Renders this expression into the current SQL translation.
	///
	/// Implementations must append only this expression and must not retain the
	/// translator, appender, or session factory after this call returns.
	///
	/// @since 8.0
	@Override
	@SPI(IMPLEMENT)
	public abstract void renderToSql(
			SqlAppender sqlAppender,
			SqlAstTranslator<?> translator,
			SessionFactoryImplementor sessionFactory);
}

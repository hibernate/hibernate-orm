/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import jakarta.annotation.Nullable;
import org.hibernate.dialect.sql.ast.spi.AbstractSelfRenderingExpression;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;

/// External provider expression implemented through the supported
/// self-rendering-expression base.
///
/// @since 8.0
/// @author Steve Ebersole
// tag::self-rendering-expression[]
public final class ExampleSelfRenderingExpression extends AbstractSelfRenderingExpression {
	/// Creates a fixture expression with the supplied mapping type.
	///
	/// @since 8.0
	public ExampleSelfRenderingExpression(@Nullable JdbcMappingContainer expressionType) {
		super( expressionType );
	}

	/// Renders the deterministic fixture expression.
	///
	/// @since 8.0
	@Override
	public void renderToSql(
			SqlAppender sqlAppender,
			SqlAstTranslator<?> translator,
			SessionFactoryImplementor sessionFactory) {
		sqlAppender.appendSql( "fixture_expression" );
	}
}
// end::self-rendering-expression[]

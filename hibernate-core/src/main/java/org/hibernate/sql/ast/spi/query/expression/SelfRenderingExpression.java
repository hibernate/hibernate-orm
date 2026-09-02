/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query.expression;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public interface SelfRenderingExpression extends Expression {
	@Override
	default void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitSelfRenderingExpression( this );
	}

	void renderToSql(SqlAppender sqlAppender, SqlAstTranslator<?> walker, SessionFactoryImplementor sessionFactory);
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.SqlAstWalker;

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

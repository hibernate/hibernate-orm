/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public interface SelfRenderingExpression extends Expression {
	@Override
	default void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitSelfRenderingExpression( this );
	}

	void renderToSql(SqlAppender sqlAppender, SqlAstWalker walker, SessionFactoryImplementor sessionFactory);
}

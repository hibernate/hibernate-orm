/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;

/**
 * A wrapper for an expression that also renders an alias.
 *
 * @author Christian Beikov
 */
public class AliasedExpression implements SelfRenderingExpression {

	private final Expression expression;
	private final String alias;

	public AliasedExpression(Expression expression, String alias) {
		this.expression = expression;
		this.alias = alias;
	}

	@Override
	public void renderToSql(SqlAppender sqlAppender, SqlAstTranslator<?> walker, SessionFactoryImplementor sessionFactory) {
		expression.accept( walker );
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( alias );
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return expression.getExpressionType();
	}

}

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
 * A wrapper for a literal to render as parameter through a cast function.
 *
 * @see org.hibernate.sql.ast.spi.AbstractSqlAstTranslator
 *
 * @author Christian Beikov
 */
public class LiteralAsParameter<T> implements SelfRenderingExpression {
	private final Literal literal;
	private final String parameterMarker;

	public LiteralAsParameter(Literal literal, String parameterMarker) {
		this.literal = literal;
		this.parameterMarker = parameterMarker;
	}

	@Override
	public void renderToSql(SqlAppender sqlAppender, SqlAstTranslator<?> walker, SessionFactoryImplementor sessionFactory) {
		sqlAppender.append( parameterMarker );
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return literal.getExpressionType();
	}

	public Literal getLiteral() {
		return literal;
	}
}

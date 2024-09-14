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
 * Represents a self rendering expression that renders a SQL fragment.
 *
 * @author Christian Beikov
 */
public class SelfRenderingSqlFragmentExpression implements SelfRenderingExpression {
	private final String expression;

	public SelfRenderingSqlFragmentExpression(String expression) {
		this.expression = expression;
	}

	public String getExpression() {
		return expression;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return null;
	}

	@Override
	public void renderToSql(
			SqlAppender sqlAppender,
			SqlAstTranslator<?> walker,
			SessionFactoryImplementor sessionFactory) {
		sqlAppender.append( expression );
	}
}
